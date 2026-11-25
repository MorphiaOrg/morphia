package dev.morphia.audits.offline

import dev.morphia.audits.extraction.ApiCoverage
import dev.morphia.audits.extraction.DocumentedOperator
import dev.morphia.audits.extraction.ExampleInventory
import dev.morphia.audits.extraction.UntestedExample
import dev.morphia.audits.extraction.UntestedExamples
import dev.morphia.audits.extraction.UntestedReason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
}
