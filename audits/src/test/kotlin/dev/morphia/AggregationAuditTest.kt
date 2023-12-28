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
import org.testng.annotations.Test

class AggregationAuditTest {
    init {
        GithubProject.dryRun = System.getenv()["GITHUB_ACTION"] == null
    }

    @Test
    fun testOperator() {
        var operator = Operator(File("${aggRoot}/count-accumulator.txt"))
    }

    @Test
    fun expressions() {
        validate(
            RstAuditor.aggregations(
                EXPRESSION,
                listOf(
                    "interface",
                    "search", // a complicated animal.  we'll get there.
                    "searchMeta", // a complicated animal.  we'll get there.
                    "substr", // deprecated/aliased away
                    "toggle-logging",
                    "vectorSearch"
                )
            )
        )
    }

    @Test
    fun stages() {
        validate(
            RstAuditor.aggregations(
                STAGE,
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
            )
        )
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
