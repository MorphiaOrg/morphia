package dev.morphia.audits.extraction

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/** A documented operator with no tagged Morphia factory, and the untested scenarios it blocks. */
data class MissingApi(
    val context: String,
    val operator: String,
    val page: String,
    val blocks: List<String>,
)

/**
 * Morphia's operator coverage, from the javadoc tags on its factories: `@aggregation.expression`,
 * `@aggregation.stage`, `@query.filter` and `@update.operator`, each followed by the operator. A
 * tag is a claim, not a proof: a tagged factory might not support every option the docs show, and
 * those option gaps surface when a test is written.
 */
object ApiCoverage {
    /** Each tag's operator family. */
    val TAGS =
        mapOf(
            "aggregation.expression" to "aggregation/expressions",
            "aggregation.stage" to "aggregation/stages",
            "query.filter" to "query/filters",
            "update.operator" to "query/updates",
        )

    private val TAG =
        Regex(
            """@(aggregation\.expression|aggregation\.stage|query\.filter|update\.operator)\s+(\$[^\s*]*)"""
        )

    /** The tagged operators of core's sources, by family. */
    fun tags(repository: Path): Map<String, Set<String>> {
        val tags = TAGS.values.associateWith { mutableSetOf<String>() }
        Files.walk(repository.resolve("core/src/main/java")).use { files ->
            files
                .filter { it.isRegularFile() && it.extension == "java" }
                .forEach { file ->
                    TAG.findAll(file.readText()).forEach { match ->
                        tags.getValue(TAGS.getValue(match.groupValues[1])) +=
                            unescape(match.groupValues[2])
                    }
                }
        }
        return tags
    }

    /**
     * A tag's operator as the docs name it: javadoc needs `$[<identifier>]` written with entities.
     */
    fun unescape(operator: String): String =
        operator.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")

    /**
     * The documented operators of the families with no tagged factory, each with the untested
     * scenarios that wait for it.
     */
    fun missing(
        inventory: ExampleInventory,
        tags: Map<String, Set<String>>,
        baseline: UntestedExamples,
    ): List<MissingApi> =
        inventory.operators
            .filter { it.context in Extraction.FAMILIES && it.token !in tags[it.context].orEmpty() }
            .groupBy { it.context to it.token }
            .map { (key, pages) ->
                MissingApi(
                    key.first,
                    key.second,
                    pages.first().page,
                    baseline.examples
                        .filter {
                            it.reason == UntestedReason.NO_MORPHIA_API &&
                                it.operator == key.second &&
                                family(it) == key.first
                        }
                        .map { it.scenario },
                )
            }

    /**
     * The baseline's operator gaps that no longer hold: their operator now has a tagged factory, so
     * the test can be written. An option gap names what a tag can't prove, so only a test clears
     * it.
     */
    fun staleGaps(baseline: UntestedExamples, tags: Map<String, Set<String>>): List<String> =
        baseline.examples
            .filter { it.reason == UntestedReason.NO_MORPHIA_API && it.option == null }
            .filter { entry -> entry.operator in tags[family(entry)].orEmpty() }
            .map {
                "${it.scenario}: ${it.operator} now has a tagged Morphia factory; make the entry " +
                    "NOT_WRITTEN, or write the test"
            }

    /**
     * The family of the operator a gap names: its own [UntestedExample.family], or the scenario's,
     * since most examples wait on an operator of their own page's family.
     */
    fun family(entry: UntestedExample): String =
        entry.family ?: Extraction.FAMILIES.single { entry.scenario.startsWith("$it/") }
}
