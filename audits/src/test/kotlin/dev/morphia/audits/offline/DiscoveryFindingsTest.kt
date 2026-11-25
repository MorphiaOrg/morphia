package dev.morphia.audits.offline

import dev.morphia.audits.extraction.BlockRole
import dev.morphia.audits.extraction.Documented
import dev.morphia.audits.extraction.DocumentedExample
import dev.morphia.audits.extraction.DocumentedPage
import dev.morphia.audits.extraction.ExampleInventory
import dev.morphia.audits.extraction.ExampleStatus
import dev.morphia.audits.extraction.FormSource
import dev.morphia.audits.extraction.MissingApi
import dev.morphia.audits.extraction.SourceBlock
import dev.morphia.audits.publish.DiscoveryFindings
import dev.morphia.audits.publish.FindingType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiscoveryFindingsTest {
    private fun block(text: String) =
        SourceBlock(BlockRole.ACTION, text, Documented.sha256(text.toByteArray()))

    private fun inventory(vararg examples: DocumentedExample) =
        ExampleInventory(
            sourceRepository = "mongodb/docs",
            commit = "c".repeat(40),
            pages =
                listOf(
                    DocumentedPage(
                        "query/filters",
                        "gt",
                        FormSource(
                            "c".repeat(40),
                            "content/manual/manual/source/reference/operator/query/gt.txt",
                        ),
                        "b".repeat(40),
                        emptyList(),
                        examples.toList(),
                    )
                ),
            skipped = emptyList(),
            summary = emptyMap(),
        )

    private val owned =
        DocumentedExample(
            "Match Quantities",
            ExampleStatus.OWNED,
            owner = "query/filters/gt/matchQuantities",
            blocks = listOf(block("db.c.find({q: {\$gt: 1}})")),
        )
    private val added =
        DocumentedExample(
            "Match Prices",
            ExampleStatus.UNSUPPORTED,
            detail = "SYNTAX",
            blocks = listOf(block("db.c.find({p: {\$gt: 1}})")),
        )

    @Test
    fun anExampleNewUpstreamIsFiledOnce() {
        val findings =
            DiscoveryFindings.findings(
                inventory(owned),
                inventory(owned, added),
                emptyList(),
                emptyList(),
                null,
            )
        assertEquals(listOf(FindingType.NEW_EXAMPLE), findings.map { it.type })
        assertTrue(
            findings.single().key.startsWith("NEW_EXAMPLE:query/filters/gt/Match Prices#"),
            findings.single().key,
        )
        // The same inventory again files nothing new.
        assertEquals(
            emptyList<Any>(),
            DiscoveryFindings.findings(
                inventory(owned, added),
                inventory(owned, added),
                emptyList(),
                emptyList(),
                null,
            ),
        )
    }

    @Test
    fun eachKindOfFindingHasAStableKey() {
        val findings =
            DiscoveryFindings.findings(
                inventory(owned),
                inventory(owned),
                listOf(
                    MissingApi(
                        "aggregation/expressions",
                        "\$sigmoid",
                        "aggregation/expressions/sigmoid",
                        listOf("aggregation/expressions/sigmoid/applySigmoid"),
                    )
                ),
                listOf("query/filters/gt \"\$exists: false\": UNSUPPORTED (SYNTAX)"),
                """
                [ERROR] dev.morphia.test.query.filters.TestGt.testMatchQuantities -- Time elapsed: 0.1 s <<< FAILURE!
                [INFO] Tests run: 3
                """
                    .trimIndent(),
            )
        assertEquals(
            listOf(
                "FAILING_AUDIT",
                "MISSING_API:aggregation/expressions/\$sigmoid",
                "MISSING_SCENARIO:query/filters/gt \"\$exists: false\"",
            ),
            findings.map { it.key },
        )
        assertTrue(
            findings[0].body.contains("dev.morphia.test.query.filters.TestGt.testMatchQuantities")
        )
        assertTrue(findings[1].body.contains("aggregation/expressions/sigmoid/applySigmoid"))
    }

    @Test
    fun passingTestsFileNothing() {
        assertEquals(
            emptyList<Any>(),
            DiscoveryFindings.findings(
                inventory(owned),
                inventory(owned),
                emptyList(),
                emptyList(),
                "[INFO] Tests run: 3, Failures: 0",
            ),
        )
    }
}
