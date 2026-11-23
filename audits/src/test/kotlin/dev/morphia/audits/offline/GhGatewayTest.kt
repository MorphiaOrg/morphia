package dev.morphia.audits.offline

import dev.morphia.audits.publish.GhException
import dev.morphia.audits.publish.GhFailure
import dev.morphia.audits.publish.GhGateway
import dev.morphia.audits.publish.GhMilestone
import dev.morphia.audits.publish.GhMode
import dev.morphia.audits.publish.GhTarget
import dev.morphia.audits.publish.IssuePatch
import dev.morphia.audits.publish.IssueRequest
import dev.morphia.audits.publish.ProcessExecutor
import dev.morphia.audits.publish.ProcessResult
import java.io.IOException
import java.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Every case runs against recorded process results: no `gh` binary, credentials or network. */
class GhGatewayTest {
    private val target = GhTarget("owner", "repo")

    /** Replays [results] in order and records each call. */
    private class Recorder(vararg results: ProcessResult) : ProcessExecutor {
        val calls = mutableListOf<Triple<List<String>, Map<String, String>, String?>>()
        private val queue = ArrayDeque(results.toList())

        override fun run(
            command: List<String>,
            environment: Map<String, String>,
            stdin: String?,
            timeout: Duration,
        ): ProcessResult {
            calls += Triple(command, environment, stdin)
            return queue.removeFirst()
        }
    }

    private fun ok(json: String) = ProcessResult(0, json, "")

    private fun gateway(
        executor: ProcessExecutor,
        mode: GhMode = GhMode.REPORT_ONLY,
        inherited: Map<String, String> = emptyMap(),
        token: String? = null,
        sleeps: MutableList<Duration> = mutableListOf(),
    ) =
        GhGateway(
            target,
            mode,
            executor,
            token = token,
            inherited = inherited,
            sleeper = { sleeps += it },
        )

    private fun issue(number: Int, extra: String = "") =
        """{"number": $number, "title": "t$number", "state": "open", "body": "b", "labels": [{"name": "audit"}], "milestone": {"id": 9000$number, "number": $number}, "html_url": "u$number"$extra}"""

    @Test
    fun issuesFlattenEveryPageAndDropPullRequests() {
        val recorder =
            Recorder(ok("[[${issue(1)}, ${issue(2, ", \"pull_request\": {}")}], [${issue(3)}]]"))
        val issues = gateway(recorder).issues()
        assertEquals(listOf(1, 3), issues.map { it.number })
        // The milestone is identified by its number, not its global id.
        assertEquals(listOf(1, 3), issues.map { it.milestone })
        assertEquals(listOf("audit"), issues.first().labels)
        val command = recorder.calls.single().first
        assertTrue(command.containsAll(listOf("--paginate", "--slurp", "--method", "GET")))
        assertEquals("repos/owner/repo/issues?state=all&per_page=100", command.last())
        assertEquals(listOf("--hostname", "github.com"), command.subList(2, 4))
    }

    @Test
    fun labelsAndMilestonesSpanPages() {
        val labels =
            gateway(Recorder(ok("""[[{"name": "a"}], [{"name": "b"}], [{"name": "c"}]]""")))
                .labels()
        assertEquals(listOf("a", "b", "c"), labels.map { it.name })
        val milestones =
            gateway(
                    Recorder(
                        ok(
                            """[[{"id": 77, "number": 1, "title": "3.0", "state": "open"}], [{"id": 78, "number": 2, "title": "2.5", "state": "closed"}]]"""
                        )
                    )
                )
                .milestones()
        assertEquals(listOf(1, 2), milestones.map { it.number })
        assertEquals("closed", milestones.last().state)
    }

    @Test
    fun reportOnlyModeNeverSpawnsAMutatingCommand() {
        val recorder = Recorder()
        val gateway = gateway(recorder)
        assertThrows(IllegalStateException::class.java) {
            gateway.create(IssueRequest("t", "b", emptyList(), null))
        }
        assertThrows(IllegalStateException::class.java) {
            gateway.update(1, IssuePatch(body = "b"))
        }
        assertThrows(IllegalStateException::class.java) { gateway.createMilestone("3.0.1") }
        assertTrue(recorder.calls.isEmpty())
    }

    @Test
    fun createMilestonePostsItsTitleOnce() {
        val created =
            Recorder(ok("""{"id": 99, "number": 102, "title": "3.0.1", "state": "open"}"""))
        val milestone = gateway(created, GhMode.PUBLISH).createMilestone("3.0.1")
        assertEquals(GhMilestone(102, "3.0.1", "open"), milestone)
        val (command, _, stdin) = created.calls.single()
        assertTrue(command.containsAll(listOf("--method", "POST", "repos/owner/repo/milestones")))
        assertEquals("""{"title":"3.0.1"}""", stdin)
    }

    @Test
    fun createSendsThePayloadOnStdinAndIsNeverReplayed() {
        val created = Recorder(ok(issue(5)))
        val issue =
            gateway(created, GhMode.PUBLISH)
                .create(IssueRequest("title", "body", listOf("audit"), 4))
        assertEquals(5, issue.number)
        val (command, _, stdin) = created.calls.single()
        assertTrue(command.containsAll(listOf("--method", "POST", "--input", "-")))
        assertEquals("""{"title":"title","body":"body","labels":["audit"],"milestone":4}""", stdin)

        // A timed-out create is reported, not retried: GitHub may already have the issue.
        val lost = Recorder(ProcessResult(-1, "", "", timedOut = true))
        val failure =
            assertThrows(GhException::class.java) {
                gateway(lost, GhMode.PUBLISH).create(IssueRequest("t", "b", emptyList(), null))
            }
        assertEquals(GhFailure.TIMEOUT, failure.failure)
        assertEquals(1, lost.calls.size)
    }

    @Test
    fun updateSendsOnlyTheChangedFields() {
        val recorder = Recorder(ok(issue(7)))
        gateway(recorder, GhMode.PUBLISH).update(7, IssuePatch(body = "new"))
        val (command, _, stdin) = recorder.calls.single()
        assertEquals("repos/owner/repo/issues/7", command.last())
        assertEquals("""{"body":"new"}""", stdin)
    }

    @Test
    fun readsRetryRateLimitsWithBackoff() {
        val sleeps = mutableListOf<Duration>()
        val recorder =
            Recorder(
                ProcessResult(1, "", "gh: API rate limit exceeded (HTTP 403)"),
                ProcessResult(1, "", "HTTP 502: Bad Gateway"),
                ok("[[]]"),
            )
        assertEquals(emptyList<Any>(), gateway(recorder, sleeps = sleeps).issues())
        assertEquals(listOf(Duration.ofSeconds(1), Duration.ofSeconds(2)), sleeps)
        // A permission failure is not retried.
        val denied = Recorder(ProcessResult(1, "", "HTTP 403: Resource not accessible"))
        val failure = assertThrows(GhException::class.java) { gateway(denied).issues() }
        assertEquals(GhFailure.PERMISSION, failure.failure)
        assertEquals(1, denied.calls.size)
    }

    @Test
    fun failuresAreClassified() {
        fun failure(result: ProcessResult) =
            assertThrows(GhException::class.java) {
                    gateway(Recorder(result, result, result)).issue(1)
                }
                .failure
        assertEquals(
            GhFailure.AUTHENTICATION,
            failure(
                ProcessResult(4, "", "To get started with GitHub CLI, please run: gh auth login")
            ),
        )
        assertEquals(GhFailure.NOT_FOUND, failure(ProcessResult(1, "", "HTTP 404: Not Found")))
        assertEquals(GhFailure.INVALID_JSON, failure(ok("{not json")))
        assertEquals(GhFailure.INVALID_JSON, failure(ok("""{"number": "1"}""")))
        val missing = ProcessExecutor { _, _, _, _ -> throw IOException("No such file") }
        assertEquals(
            GhFailure.MISSING_BINARY,
            assertThrows(GhException::class.java) { gateway(missing).issue(1) }.failure,
        )
    }

    @Test
    fun theVersionMustBeInTheTestedRange() {
        gateway(Recorder(ok("gh version 2.92.0 (2026-04-28)\n"))).checkVersion()
        assertEquals(
            GhFailure.UNSUPPORTED_VERSION,
            assertThrows(GhException::class.java) {
                    gateway(Recorder(ok("gh version 2.10.0 (2022-01-01)\n"))).checkVersion()
                }
                .failure,
        )
    }

    @Test
    fun theChildEnvironmentSelectsHostRepositoryAndTokenExplicitly() {
        val recorder = Recorder(ok(issue(1)))
        gateway(
                recorder,
                inherited =
                    mapOf(
                        "PATH" to "/bin",
                        "GH_HOST" to "elsewhere.example",
                        "GH_REPO" to "other/repo",
                        "GITHUB_TOKEN" to "ambient",
                        "GH_DEBUG" to "api",
                    ),
                token = "chosen",
            )
            .issue(1)
        val environment = recorder.calls.single().second
        assertEquals("/bin", environment["PATH"])
        assertEquals("chosen", environment["GH_TOKEN"])
        listOf("GH_HOST", "GH_REPO", "GITHUB_TOKEN", "GH_DEBUG").forEach {
            assertFalse(it in environment, it)
        }
        assertEquals("1", environment["GH_PROMPT_DISABLED"])
        assertEquals("", environment["GH_PAGER"])
    }
}
