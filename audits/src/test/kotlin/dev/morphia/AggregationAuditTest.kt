package dev.morphia

import dev.morphia.audits.GithubProject
import dev.morphia.audits.RstAuditor
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
    companion object {
        val EXPRESSION_IGNORES =
            listOf(
                "interface",
                "search", // a complicated animal.  we'll get there.
                "searchMeta", // a complicated animal.  we'll get there.
                "substr", // deprecated/aliased away
                "toggle-logging",
                "vectorSearch"
            )
        val STAGE_IGNORES =
            listOf(
                "changeStream",
                "changeStreamSplitLargeEvent",
                "collStats",
                "listLocalSessions",
                "listSampledQueries",
                "listSearchIndexes",
                "listSessions",
                "queryStats", // unclear how this would be needed in morphia
                "shardedDataDistribution",
                "toHashedIndexKey",
                "vectorSearch",
            )
    }

    init {
        GithubProject.dryRun = System.getenv()["GITHUB_ACTION"] == null
    }

    @Test
    fun testOperator() {
        var operator = Operator(File("${aggRoot}/unsetField.txt"))
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
