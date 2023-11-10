package dev.morphia

import dev.morphia.model.Operator
import dev.morphia.model.Results
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

class RstAuditorTest {
    init {
        GithubProject.dryRun = System.getenv()["GITHUB_ACTION"] == null
    }

    @Test
    fun testOperator() {
        var operator = Operator("firstN-array-element")
    }

    @Test
    fun aggregationExpressions() {
        val results =
            RstAuditor("@aggregation.expression")
                .aggregations(
                    listOf(
                        "\$interface",
                        "\$search", // a complicated animal.  we'll get there.
                        "\$searchMeta", // a complicated animal.  we'll get there.
                        "\$substr" // deprecated/aliased away
                    )
                )

        validate(results)
    }

    @Test
    fun aggregationPipelineStages() {
        val results = RstAuditor("@aggregation.stage").aggregations(listOf("\$collStats"))
        validate(results)
    }

    private fun validate(results: Results) {
        assertEquals(
            results.created.size,
            0,
            "All existing operators should be represented: " +
                results.created.joinToString("\n\t", prefix = "\n\t") { it.operator }
        )
        assertEquals(
            results.noExamples.size,
            0,
            "All existing operators should have examples: " +
                results.noExamples.joinToString("\n\t", prefix = "\n\t") {
                    "${it.operator}: ${it.name}"
                }
        )
    }
}
