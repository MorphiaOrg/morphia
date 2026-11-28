package dev.morphia.audits.extraction

import com.fasterxml.jackson.annotation.JsonInclude
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import org.bson.BsonDocument

/** Why a scenario has no test yet. */
enum class UntestedReason {
    /** Nobody has written the test yet: the work queue. */
    NOT_WRITTEN,
    /**
     * The example uses an operator or option Morphia has no API for; [UntestedExample.detail] names
     * it.
     */
    NO_MORPHIA_API,
    /** Admin and diagnostic stages, change streams and the like: reported, never queued. */
    OUT_OF_SCOPE,
}

/**
 * One reviewed untested scenario: its name (`<context>/<operator>/<stem>`) and why it is untested.
 */
data class UntestedExample(
    val scenario: String,
    val reason: UntestedReason,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val detail: String? = null,
    /** For `NO_MORPHIA_API`: the operator Morphia lacks, or whose [option] it lacks. */
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val operator: String? = null,
    /** For an option gap: the option Morphia's factory for [operator] doesn't support. */
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val option: String? = null,
    /**
     * The family of [operator] when it isn't the scenario's own, such as a query operator a
     * `$match` example needs: `query/filters`.
     */
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val family: String? = null,
)

/** `audits/catalog/untested-examples.json`: the reviewed baseline of untested scenarios. */
data class UntestedExamples(val schemaVersion: Int = 1, val examples: List<UntestedExample>)

/**
 * An in-scope example, or a page, with no scenario: [line] says why; [key] names it stably. The
 * other fields are for [MissingReport]: the [page] (`<context>/<operator>`), the example's [title]
 * (null when the reader couldn't read the page at all), the [category] the report groups it under,
 * the reader's [detail], and the example's source [blocks].
 */
data class MissingScenario(
    val key: String,
    val line: String,
    val page: String = "",
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val title: String? = null,
    val category: String = "",
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val detail: String? = null,
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) val blocks: List<SourceBlock> = emptyList(),
)

/** One entry of the work queue: a scenario with no test, and the test it needs. */
data class UntestedScenario(
    val scenario: String,
    val path: String,
    val title: String,
    val operator: String,
    val reason: UntestedReason?,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val detail: String?,
    /**
     * The test the scenario needs, as `<class>#<method>`, by the naming rule core's lookup uses.
     */
    val test: String,
)

/**
 * The audit's two completeness checks: every in-scope documented example has a scenario or a
 * reviewed exclusion, and every scenario has its test or a reviewed baseline entry.
 */
object Coverage {
    const val UNTESTED = "audits/catalog/untested-examples.json"
    private const val RESOURCES = "core/src/test/resources/dev/morphia/test"
    private const val SOURCES = "core/src/test/java/dev/morphia/test"

    /**
     * The in-scope examples with neither a scenario nor an exclusion, one line each. There is no
     * baseline: an example the reader can't extract is a failure until it learns it, or a reviewer
     * excludes the example.
     */
    fun missingScenarios(
        repository: Path,
        inventory: ExampleInventory,
        overrides: ExampleOverrides,
    ): List<String> = missing(repository, inventory, overrides).map { it.line }

    /** [missingScenarios], each with a key that stays the same while the example does. */
    fun missing(
        repository: Path,
        inventory: ExampleInventory,
        overrides: ExampleOverrides,
    ): List<MissingScenario> {
        val excluded =
            overrides.overrides
                .filter { it.exclude }
                .map { Triple(it.page, it.title, it.blocksSha256) }
                .toSet()
        return inventory.pages
            .filter { it.context in Extraction.FAMILIES }
            .flatMap { page ->
                val problems =
                    page.problems.mapIndexed { index, problem ->
                        val code = problem.substringBefore(':')
                        // A code the page reports twice keeps the two apart by order.
                        val ordinal =
                            page.problems.take(index).count { it.substringBefore(':') == code }
                        MissingScenario(
                            "${page.id}#$code" + if (ordinal > 0) "#${ordinal + 1}" else "",
                            "${page.id}: the reader couldn't read the page: $problem",
                            page.id,
                            category = code,
                            detail = problem.substringAfter(':', "").trim().ifEmpty { null },
                        )
                    }
                problems +
                    page.examples
                        .zip(exampleKeys(page))
                        // An owned example must still have its scenario.
                        .filter { (example, _) ->
                            example.status != ExampleStatus.OWNED ||
                                !Files.isRegularFile(
                                    repository.resolve("$RESOURCES/${example.owner}/scenario.json")
                                )
                        }
                        .filter { (example, _) ->
                            Triple(page.id, example.title, Overrides.identity(example.blocks)) !in
                                excluded
                        }
                        .map { (example, key) ->
                            MissingScenario(
                                key,
                                "${page.id} \"${example.title}\": ${example.status}" +
                                    (example.detail?.let { " ($it)" } ?: ""),
                                page.id,
                                example.title,
                                category(example),
                                example.detail,
                                example.blocks,
                            )
                        }
            }
    }

    /**
     * What a missing example waits on: the reader's problem code for an unsupported one, its status
     * otherwise, and `SCENARIO_FILE_MISSING` for an owned example whose scenario is gone.
     */
    private fun category(example: DocumentedExample): String =
        when (example.status) {
            ExampleStatus.UNSUPPORTED ->
                example.detail?.substringBefore(':')?.trim()?.ifEmpty { null } ?: "UNSUPPORTED"
            ExampleStatus.OWNED -> "SCENARIO_FILE_MISSING"
            else -> example.status.name
        }

    /**
     * Each example's key: its page and title, and its place among the page's examples with that
     * title when it isn't the first. It leaves the example's source out, so an edit upstream keeps
     * the key, and an issue filed for it is updated rather than filed again.
     */
    fun exampleKeys(page: DocumentedPage): List<String> =
        page.examples.mapIndexed { index, example ->
            val ordinal =
                page.examples.take(index).count { it.title.trim() == example.title.trim() }
            "${page.id}/${example.title.trim()}" + if (ordinal > 0) "#${ordinal + 1}" else ""
        }

    /**
     * Every committed scenario of the operator families with no test method, as the work queue
     * lists it.
     */
    fun untested(repository: Path): List<UntestedScenario> =
        Extraction.FAMILIES.sorted().flatMap { family ->
            val base = repository.resolve("$RESOURCES/$family")
            if (!base.isDirectory()) return@flatMap emptyList()
            Files.walk(base, 3)
                .use { files ->
                    files
                        .filter { it.name == "scenario.json" && it.isRegularFile() }
                        .sorted()
                        .toList()
                }
                .mapNotNull { file ->
                    val operator = file.parent.parent.name
                    val stem = file.parent.name
                    val type = "Test" + operator.replaceFirstChar { it.uppercase() }
                    val method = "test" + stem.replaceFirstChar { it.uppercase() }
                    val source = repository.resolve("$SOURCES/$family/$type.java")
                    val tested =
                        source.isRegularFile() &&
                            Regex("""\bvoid\s+${Regex.escape(method)}\s*\(""")
                                .containsMatchIn(source.readText())
                    if (tested) return@mapNotNull null
                    val scenario = BsonDocument.parse(file.readText())
                    UntestedScenario(
                        scenario.getString("name").value,
                        repository.relativize(file).toString(),
                        scenario.getString("title").value,
                        operator,
                        null,
                        null,
                        "dev.morphia.test.${family.replace('/', '.')}.$type#$method",
                    )
                }
        }

    /**
     * The untested scenarios with their baseline reasons, and the problems: a new untested scenario
     * the baseline lacks, or a baseline entry whose scenario now has its test or no longer exists.
     */
    fun baseline(
        untested: List<UntestedScenario>,
        baseline: UntestedExamples,
    ): Pair<List<UntestedScenario>, List<String>> {
        val reviewed = baseline.examples.associateBy { it.scenario }
        val problems = mutableListOf<String>()
        baseline.examples
            .groupingBy { it.scenario }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { problems += "$it is in the untested baseline more than once" }
        baseline.examples
            .filter {
                val gap = it.reason == UntestedReason.NO_MORPHIA_API
                gap != !it.operator.isNullOrBlank() ||
                    it.option != null && (!gap || it.option.isBlank()) ||
                    it.family != null && (!gap || it.family !in Extraction.FAMILIES)
            }
            .forEach {
                problems +=
                    "${it.scenario}: NO_MORPHIA_API names the operator Morphia lacks (and its family when it isn't the scenario's), or that operator and its missing option; no other reason does"
            }
        val names = untested.map { it.scenario }.toSet()
        (reviewed.keys - names).sorted().forEach {
            problems +=
                "$it is in the untested baseline, but it has its test now or no longer exists: remove it"
        }
        val merged =
            untested.map { entry ->
                val review = reviewed[entry.scenario]
                if (review == null) {
                    problems +=
                        "${entry.scenario} has no test: write ${entry.test}, or add it to $UNTESTED with a reason"
                }
                entry.copy(reason = review?.reason, detail = review?.detail)
            }
        return merged to problems
    }

    /** The summary every audit run prints: untested scenarios by reason. */
    fun summary(untested: List<UntestedScenario>): String = buildString {
        appendLine("Untested scenarios: ${untested.size}")
        untested
            .groupingBy { it.reason?.name ?: "NEW" }
            .eachCount()
            .toSortedMap()
            .forEach { (reason, count) -> appendLine("  $reason: $count") }
    }
}
