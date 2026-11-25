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
)

/** `audits/catalog/untested-examples.json`: the reviewed baseline of untested scenarios. */
data class UntestedExamples(val schemaVersion: Int = 1, val examples: List<UntestedExample>)

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
    ): List<String> {
        val excluded =
            overrides.overrides
                .filter { it.exclude }
                .map { Triple(it.page, it.title, it.blocksSha256) }
                .toSet()
        return inventory.pages
            .filter { it.context in Extraction.FAMILIES }
            .flatMap { page ->
                page.problems.map { "${page.id}: the reader couldn't read the page: $it" } +
                    page.examples
                        // An owned example must still have its scenario.
                        .filter {
                            it.status != ExampleStatus.OWNED ||
                                !Files.isRegularFile(
                                    repository.resolve("$RESOURCES/${it.owner}/scenario.json")
                                )
                        }
                        .filter {
                            Triple(page.id, it.title, Overrides.identity(it.blocks)) !in excluded
                        }
                        .map {
                            "${page.id} \"${it.title}\": ${it.status}${it.detail?.let { d -> " ($d)" } ?: ""}"
                        }
            }
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
                (it.reason == UntestedReason.NO_MORPHIA_API) != (it.operator != null) ||
                    it.option != null && it.operator == null
            }
            .forEach {
                problems +=
                    "${it.scenario}: NO_MORPHIA_API names the operator Morphia lacks, or that operator and its missing option; no other reason does"
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
