package dev.morphia

import dev.morphia.audits.GithubProject
import dev.morphia.audits.RstAuditor
import dev.morphia.audits.RstAuditor.aggRoot
import dev.morphia.audits.model.Operator
import dev.morphia.audits.model.Results
import java.io.File
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

class RstAuditorTest {
    init {
        GithubProject.dryRun = System.getenv()["GITHUB_ACTION"] == null
    }

    @Test
    fun testOperator() {
        var operator = Operator(File("${aggRoot}/count-accumulator.txt"))
    }

    @Test
    fun aggregationOperators() {
        validate(
            RstAuditor.aggregations(
                listOf(
                    // stages
                    "changeStream",
                    "changeStreamSplitLargeEvent",
                    "listLocalSessions",
                    "listSampledQueries",
                    "listSearchIndexes",
                    "listSessions",
                    "toHashedIndexKey",
                    "shardedDataDistribution",
                    // expressions
                    "collStats",
                    "interface",
                    "queryStats", // unclear how this would be needed in morphia
                    "search", // a complicated animal.  we'll get there.
                    "searchMeta", // a complicated animal.  we'll get there.
                    "substr", // deprecated/aliased away
                    "toggle-logging",
                    "vectorSearch"
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
