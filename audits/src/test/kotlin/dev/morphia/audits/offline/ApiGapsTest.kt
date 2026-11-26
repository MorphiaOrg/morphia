package dev.morphia.audits.offline

import dev.morphia.audits.extraction.ApiCoverage
import dev.morphia.audits.extraction.Coverage
import dev.morphia.audits.extraction.DocumentedOperator
import dev.morphia.audits.extraction.ExampleInventory
import dev.morphia.audits.extraction.UntestedExample
import dev.morphia.audits.extraction.UntestedExamples
import dev.morphia.audits.extraction.UntestedReason
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ApiGapsTest {
    private val gap =
        UntestedExample(
            "aggregation/expressions/sigmoid/applySigmoid",
            UntestedReason.NO_MORPHIA_API,
            operator = "\$sigmoid",
        )
    private val optionGap =
        UntestedExample(
            "aggregation/expressions/filter/useArrayIndexAs",
            UntestedReason.NO_MORPHIA_API,
            operator = "\$filter",
            option = "arrayIndexAs",
        )

    @Test
    fun anOperatorGapClearsOnceTheOperatorIsTagged() {
        val baseline = UntestedExamples(examples = listOf(gap, optionGap))
        assertEquals(
            emptyList<String>(),
            ApiCoverage.staleGaps(baseline, mapOf("aggregation/expressions" to setOf("\$filter"))),
        )
        assertEquals(
            listOf(
                "aggregation/expressions/sigmoid/applySigmoid: \$sigmoid now has a tagged Morphia factory; make the entry NOT_WRITTEN, or write the test"
            ),
            ApiCoverage.staleGaps(
                baseline,
                mapOf("aggregation/expressions" to setOf("\$sigmoid", "\$filter")),
            ),
        )
    }

    @Test
    fun anUntaggedOperatorIsMissingWithTheScenariosItBlocks() {
        val inventory =
            ExampleInventory(
                sourceRepository = "mongodb/docs",
                commit = "a".repeat(40),
                pages = emptyList(),
                skipped = emptyList(),
                summary = emptyMap(),
                operators =
                    listOf(
                        DocumentedOperator(
                            "aggregation/expressions",
                            "\$sigmoid",
                            "aggregation/expressions/sigmoid",
                        ),
                        DocumentedOperator(
                            "aggregation/expressions",
                            "\$abs",
                            "aggregation/expressions/abs",
                        ),
                        // Outside the families core's tests cover.
                        DocumentedOperator(
                            "aggregation/variables",
                            "\$\$NOW",
                            "aggregation/variables/now",
                        ),
                    ),
            )
        val missing =
            ApiCoverage.missing(
                inventory,
                mapOf("aggregation/expressions" to setOf("\$abs")),
                UntestedExamples(examples = listOf(gap)),
            )
        assertEquals(listOf("\$sigmoid"), missing.map { it.operator })
        assertEquals(listOf(gap.scenario), missing.single().blocks)
    }

    @Test
    fun aGapCanNameAnotherFamilysOperator() {
        val exists =
            UntestedExample(
                "aggregation/stages/match/matchExistingFields",
                UntestedReason.NO_MORPHIA_API,
                operator = "\$exists",
                family = "query/filters",
            )
        assertEquals(
            1,
            ApiCoverage.staleGaps(
                    UntestedExamples(examples = listOf(exists)),
                    mapOf("query/filters" to setOf("\$exists")),
                )
                .size,
        )
        assertEquals(
            emptyList<String>(),
            ApiCoverage.staleGaps(
                UntestedExamples(examples = listOf(exists)),
                mapOf("aggregation/stages" to setOf("\$exists")),
            ),
        )
    }

    @Test
    fun tagTokensEndAtWhitespaceOrTheComment(@TempDir repository: Path) {
        val source = repository.resolve("core/src/main/java/Ops.java")
        Files.createDirectories(source.parent)
        Files.writeString(
            source,
            """
            /** @aggregation.expression ${'$'}abs*/
            /**
             * @update.operator ${'$'}
             * @query.filter ${'$'}eq
             */
            """
                .trimIndent(),
        )
        val tags = ApiCoverage.tags(repository)
        assertEquals(setOf("\$abs"), tags["aggregation/expressions"])
        assertEquals(setOf("\$"), tags["query/updates"])
        assertEquals(setOf("\$eq"), tags["query/filters"])
    }

    @Test
    fun aGapNamesWhatIsMissing() {
        val blank =
            UntestedExample(
                "aggregation/expressions/abs/x",
                UntestedReason.NO_MORPHIA_API,
                operator = "",
            )
        val blankOption =
            UntestedExample(
                "aggregation/expressions/abs/y",
                UntestedReason.NO_MORPHIA_API,
                operator = "\$abs",
                option = " ",
            )
        val (_, problems) =
            Coverage.baseline(emptyList(), UntestedExamples(examples = listOf(blank, blankOption)))
        assertEquals(2, problems.count { "NO_MORPHIA_API names" in it }, problems.toString())
    }
}
