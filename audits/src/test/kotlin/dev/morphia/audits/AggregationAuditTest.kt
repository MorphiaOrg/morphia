package dev.morphia.audits

import dev.morphia.audits.RstAuditor.aggRoot
import dev.morphia.audits.model.Operator
import dev.morphia.audits.model.OperatorType.EXPRESSION
import dev.morphia.audits.model.OperatorType.STAGE
import dev.morphia.audits.model.Results
import java.io.File
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.Test

class AggregationAuditTest {
    init {
        GithubProject.dryRun = System.getenv()["GITHUB_ACTION"] == null
    }

    @Test
    fun testOperator() {
        val name = "merge"
        var operator = Operator(File("${aggRoot}/$name.txt"))
        operator.examples.forEach { it.output(File("target/testOperator-${name}/${it.name}")) }
    }

    @Test
    fun expressions() {
        validate(RstAuditor.aggregations(EXPRESSION))
    }

    @Test
    fun stages() {
        validate(RstAuditor.aggregations(STAGE))
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
        val noTags = results.noServerRelease.joinToString("\n", "\n")
        assertTrue(
            noTags.trim().isEmpty(),
            "Some operators are missing server release tags: ${noTags}"
        )
    }
}
