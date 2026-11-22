package dev.morphia.audits.publish

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit

/** One finished child process: its exit status and both streams. */
data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean = false,
)

/** Runs a command as an argument array; tests substitute recorded results. */
fun interface ProcessExecutor {
    fun run(
        command: List<String>,
        environment: Map<String, String>,
        stdin: String?,
        timeout: Duration,
    ): ProcessResult
}

/** [ProcessBuilder] with a replaced environment, a bounded wait and bounded captured output. */
class SystemProcessExecutor(private val maximumOutput: Int = 64 * 1024 * 1024) : ProcessExecutor {
    override fun run(
        command: List<String>,
        environment: Map<String, String>,
        stdin: String?,
        timeout: Duration,
    ): ProcessResult {
        val builder = ProcessBuilder(command)
        builder.environment().clear()
        builder.environment().putAll(environment)
        val process = builder.start()
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val readers =
            listOf(process.inputStream to stdout, process.errorStream to stderr).map {
                (stream, target) ->
                Thread {
                        stream.bufferedReader().use { reader ->
                            val buffer = CharArray(8192)
                            while (true) {
                                val read = reader.read(buffer)
                                if (read < 0) break
                                // Keep reading past the limit so the child never blocks on a full
                                // pipe, but keep only a bounded amount.
                                val room = maximumOutput - target.length
                                if (room > 0) target.append(buffer, 0, minOf(read, room))
                            }
                        }
                    }
                    .apply { start() }
            }
        process.outputStream.use { input -> stdin?.let { input.write(it.toByteArray()) } }
        val finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroyForcibly()
            process.waitFor()
        }
        readers.forEach { it.join(1000) }
        return ProcessResult(
            if (finished) process.exitValue() else -1,
            stdout.toString(),
            stderr.toString(),
            timedOut = !finished,
        )
    }
}

/** Why a `gh` call failed; callers decide which are worth a retry. */
enum class GhFailure {
    MISSING_BINARY,
    UNSUPPORTED_VERSION,
    AUTHENTICATION,
    PERMISSION,
    NOT_FOUND,
    RATE_LIMITED,
    TIMEOUT,
    SERVER_ERROR,
    INVALID_JSON,
    FAILED,
}

class GhException(val failure: GhFailure, message: String) : RuntimeException(message)

/** The repository a gateway reads and writes, on an explicit host. */
data class GhTarget(val owner: String, val repository: String, val host: String = "github.com") {
    init {
        val name = Regex("[A-Za-z0-9_.-]+")
        require(name.matches(owner) && name.matches(repository) && name.matches(host)) {
            "Invalid target $host/$owner/$repository"
        }
    }

    val path = "repos/$owner/$repository"
}

/**
 * Whether a gateway may change anything. `REPORT_ONLY` never spawns a mutating command: the generic
 * `gh api` command has no dry run of its own, so the gateway enforces it.
 */
enum class GhMode {
    REPORT_ONLY,
    PUBLISH,
}

data class GhIssue(
    val number: Int,
    val title: String,
    val state: String,
    val body: String,
    val labels: List<String>,
    /** The milestone's `number`, which the issues API uses, never its global `id`. */
    val milestone: Int?,
    val url: String,
)

data class GhLabel(val name: String)

data class GhMilestone(val number: Int, val title: String, val state: String)

/** The publisher-owned fields of a new issue. */
data class IssueRequest(
    val title: String,
    val body: String,
    val labels: List<String>,
    val milestone: Int?,
)

/** The publisher-owned fields an update changes; a null field is left as it is. */
data class IssuePatch(
    val title: String? = null,
    val body: String? = null,
    val labels: List<String>? = null,
    val milestone: Int? = null,
)

/**
 * The GitHub operations the audit publisher needs, through the `gh` CLI. Every call is an argument
 * array with an explicit host and repository; inherited `GH_HOST`/`GH_REPO` and unrelated tokens
 * are removed from the child's environment, and prompts, pagers and update notices are off. Reads
 * are retried with bounded backoff on rate limits, timeouts and server errors; writes are never
 * replayed, since a lost response does not mean the request was not received.
 */
class GhGateway(
    private val target: GhTarget,
    private val mode: GhMode,
    private val executor: ProcessExecutor = SystemProcessExecutor(),
    private val executable: String = "gh",
    /** A token for [target]'s host; null uses `gh`'s own stored credentials. */
    private val token: String? = null,
    private val inherited: Map<String, String> = System.getenv(),
    private val timeout: Duration = Duration.ofSeconds(60),
    private val attempts: Int = 3,
    private val sleeper: (Duration) -> Unit = { Thread.sleep(it.toMillis()) },
) {
    private val mapper = ObjectMapper()

    /** Fails unless `gh` is present and within the tested version range. */
    fun checkVersion() {
        val result = execute(listOf(executable, "--version"), null)
        if (result.exitCode != 0) throw failure(result)
        val version =
            Regex("""gh version (\d+)\.(\d+)\.(\d+)""").find(result.stdout)?.destructured
                ?: throw GhException(GhFailure.UNSUPPORTED_VERSION, "Unrecognized gh version")
        val (major, minor) = version.toList().map { it.toInt() }
        if (major != 2 || minor < MINIMUM_MINOR) {
            throw GhException(
                GhFailure.UNSUPPORTED_VERSION,
                "gh ${version.toList().joinToString(".")} is outside the tested range " +
                    "2.$MINIMUM_MINOR to 2.x",
            )
        }
    }

    /** Every issue, open and closed, without pull requests (the issues endpoint returns both). */
    fun issues(): List<GhIssue> =
        pages("${target.path}/issues?state=all&per_page=100")
            .filter { !it.has("pull_request") }
            .map { issue(it) }

    fun issue(number: Int): GhIssue = issue(read("GET", "${target.path}/issues/$number"))

    fun labels(): List<GhLabel> =
        pages("${target.path}/labels?per_page=100").map { GhLabel(it.text("name")) }

    fun milestones(): List<GhMilestone> =
        pages("${target.path}/milestones?state=all&per_page=100").map {
            GhMilestone(it.integer("number"), it.text("title"), it.text("state"))
        }

    /** Creates an issue once; an uncertain result is the caller's to reconcile, not retry. */
    fun create(request: IssueRequest): GhIssue {
        val payload =
            mapper.createObjectNode().apply {
                put("title", request.title)
                put("body", request.body)
                putArray("labels").apply { request.labels.forEach { add(it) } }
                request.milestone?.let { put("milestone", it) }
            }
        return issue(write("POST", "${target.path}/issues", payload.toString()))
    }

    fun update(number: Int, patch: IssuePatch): GhIssue {
        val payload =
            mapper.createObjectNode().apply {
                patch.title?.let { put("title", it) }
                patch.body?.let { put("body", it) }
                patch.labels?.let { labels ->
                    putArray("labels").apply { labels.forEach { add(it) } }
                }
                patch.milestone?.let { put("milestone", it) }
            }
        return issue(write("PATCH", "${target.path}/issues/$number", payload.toString()))
    }

    private fun pages(path: String): List<JsonNode> {
        // --slurp wraps every page in one array; it cannot be combined with --jq, so the pages are
        // flattened here.
        val slurped = read("GET", path, paginate = true)
        if (slurped !is ArrayNode) throw invalid("Expected an array of pages from $path")
        return slurped.flatMap { page ->
            if (page !is ArrayNode) throw invalid("Expected a page array from $path")
            page.toList()
        }
    }

    private fun read(method: String, path: String, paginate: Boolean = false): JsonNode {
        require(method == "GET")
        var delay = Duration.ofSeconds(1)
        repeat(attempts - 1) {
            try {
                return call(method, path, null, paginate)
            } catch (failure: GhException) {
                if (failure.failure !in retryable) throw failure
                sleeper(delay)
                delay = delay.multipliedBy(2)
            }
        }
        return call(method, path, null, paginate)
    }

    private fun write(method: String, path: String, input: String): JsonNode {
        // Report-only gateways never spawn a mutating command.
        check(mode == GhMode.PUBLISH) { "A report-only gateway cannot $method $path" }
        check(
            method == "POST" && path == "${target.path}/issues" ||
                method == "PATCH" && Regex("""\Q${target.path}\E/issues/\d+""").matches(path)
        ) {
            "$method $path is not a publisher operation"
        }
        return call(method, path, input, paginate = false)
    }

    private fun call(method: String, path: String, input: String?, paginate: Boolean): JsonNode {
        val command =
            listOf(
                executable,
                "api",
                "--hostname",
                target.host,
                "--method",
                method,
                "-H",
                "Accept: application/vnd.github+json",
            ) +
                (if (paginate) listOf("--paginate", "--slurp") else emptyList()) +
                (if (input != null) listOf("--input", "-") else emptyList()) +
                path
        val result = execute(command, input)
        if (result.exitCode != 0 || result.timedOut) throw failure(result)
        return try {
            mapper.readTree(result.stdout) ?: throw invalid("Empty response from $method $path")
        } catch (failure: com.fasterxml.jackson.core.JacksonException) {
            throw invalid("Invalid JSON from $method $path: ${failure.originalMessage}")
        }
    }

    private fun execute(command: List<String>, stdin: String?): ProcessResult =
        try {
            executor.run(command, environment(), stdin, timeout)
        } catch (failure: IOException) {
            throw GhException(
                GhFailure.MISSING_BINARY,
                "Cannot run $executable: ${failure.message}",
            )
        }

    /** The child's environment: the inherited one with `gh`'s selection made explicit. */
    internal fun environment(): Map<String, String> {
        val environment =
            inherited.filterKeys {
                it !in
                    setOf(
                        "GH_HOST",
                        "GH_REPO",
                        "GH_TOKEN",
                        "GITHUB_TOKEN",
                        "GH_ENTERPRISE_TOKEN",
                        "GITHUB_ENTERPRISE_TOKEN",
                        "GH_DEBUG",
                        "DEBUG",
                    )
            }
        return environment +
            mapOf(
                "GH_PROMPT_DISABLED" to "1",
                "GH_NO_UPDATE_NOTIFIER" to "1",
                "GH_SPINNER_DISABLED" to "1",
                "GH_PAGER" to "",
                "NO_COLOR" to "1",
            ) +
            (token?.let {
                mapOf(
                    (if (target.host == "github.com") "GH_TOKEN" else "GH_ENTERPRISE_TOKEN") to it
                )
            } ?: emptyMap())
    }

    private fun failure(result: ProcessResult): GhException {
        // Only the start of stderr is kept; it never carries the token, which goes through the
        // environment.
        val stderr = result.stderr.take(2000)
        val kind =
            when {
                result.timedOut -> GhFailure.TIMEOUT
                "rate limit" in stderr.lowercase() || "HTTP 429" in stderr -> GhFailure.RATE_LIMITED
                "HTTP 401" in stderr || "gh auth login" in stderr -> GhFailure.AUTHENTICATION
                "HTTP 403" in stderr -> GhFailure.PERMISSION
                "HTTP 404" in stderr -> GhFailure.NOT_FOUND
                Regex("""HTTP 5\d\d""").containsMatchIn(stderr) -> GhFailure.SERVER_ERROR
                else -> GhFailure.FAILED
            }
        return GhException(kind, "gh exited with ${result.exitCode}: $stderr")
    }

    private fun invalid(message: String) = GhException(GhFailure.INVALID_JSON, message)

    private fun issue(node: JsonNode): GhIssue =
        GhIssue(
            number = node.integer("number"),
            title = node.text("title"),
            state = node.text("state"),
            body = node.get("body")?.takeIf { !it.isNull }?.asText() ?: "",
            labels = (node.get("labels") ?: mapper.createArrayNode()).map { it.text("name") },
            milestone = node.get("milestone")?.takeIf { !it.isNull }?.integer("number"),
            url = node.text("html_url"),
        )

    private fun JsonNode.text(field: String): String =
        get(field)?.takeIf { it.isTextual }?.asText() ?: throw invalid("Missing text field $field")

    private fun JsonNode.integer(field: String): Int =
        get(field)?.takeIf { it.isInt }?.asInt() ?: throw invalid("Missing number field $field")

    companion object {
        /** The oldest tested `gh` 2.x minor version. */
        const val MINIMUM_MINOR = 40

        private val retryable =
            setOf(GhFailure.RATE_LIMITED, GhFailure.TIMEOUT, GhFailure.SERVER_ERROR)
    }
}
