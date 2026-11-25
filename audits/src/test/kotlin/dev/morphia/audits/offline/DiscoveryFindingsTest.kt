package dev.morphia.audits.offline

import dev.morphia.audits.extraction.BlockRole
import dev.morphia.audits.extraction.Documented
import dev.morphia.audits.extraction.DocumentedExample
import dev.morphia.audits.extraction.DocumentedPage
import dev.morphia.audits.extraction.ExampleInventory
import dev.morphia.audits.extraction.ExampleStatus
import dev.morphia.audits.extraction.FormSource
import dev.morphia.audits.extraction.MissingApi
import dev.morphia.audits.extraction.MissingScenario
import dev.morphia.audits.extraction.SourceBlock
import dev.morphia.audits.publish.DiscoveryFindings
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

    private val log =
        """
        [ERROR] dev.morphia.test.query.filters.TestGt.testMatchQuantities -- Time elapsed: 0.1 s <<< FAILURE!
        [ERROR] dev.morphia.test.query.filters.TestGt.testMatchPrices -- Time elapsed: 0.1 s <<< ERROR!
        [INFO] Tests run: 3
        """
            .trimIndent()

    private fun findings(
        committed: ExampleInventory,
        current: ExampleInventory,
        missingApi: List<MissingApi> = emptyList(),
        missing: List<MissingScenario> = emptyList(),
        auditLog: String? = null,
        extraction: String? = null,
    ) = DiscoveryFindings.findings(committed, current, missingApi, missing, auditLog, extraction)

    @Test
    fun anExampleNewUpstreamIsFiledOnce() {
        val found = findings(inventory(owned), inventory(owned, added))
        assertEquals(listOf("NEW_EXAMPLE:query/filters/gt/Match Prices"), found.map { it.key })
        assertEquals(emptyList<Any>(), findings(inventory(owned, added), inventory(owned, added)))
    }

    @Test
    fun anEditUpstreamKeepsTheKey() {
        val edited = added.copy(blocks = listOf(block("db.c.find({p: {\$gt: 2}})")))
        assertEquals(
            findings(inventory(owned), inventory(owned, added)).map { it.key },
            findings(inventory(owned), inventory(owned, edited)).map { it.key },
        )
    }

    @Test
    fun aNewlyExtractedExampleStillNeedsItsTest() {
        val extracted =
            added.copy(status = ExampleStatus.OWNED, owner = "query/filters/gt/matchPrices")
        val found = findings(inventory(owned), inventory(owned, extracted)).single()
        assertEquals("NEW_EXAMPLE:query/filters/gt/Match Prices", found.key)
        assertTrue(found.body.contains("write its test"), found.body)
    }

    @Test
    fun examplesSharingATitleKeepDistinctKeys() {
        val twin = added.copy(blocks = listOf(block("db.c.find({p: {\$gt: 3}})")))
        val keys = findings(inventory(), inventory(added, twin)).map { it.key }
        assertEquals(
            listOf(
                "NEW_EXAMPLE:query/filters/gt/Match Prices",
                "NEW_EXAMPLE:query/filters/gt/Match Prices#2",
            ),
            keys,
        )
    }

    @Test
    fun eachFailingTestIsItsOwnFinding() {
        assertEquals(
            listOf(
                "FAILING_AUDIT:dev.morphia.test.query.filters.TestGt.testMatchPrices",
                "FAILING_AUDIT:dev.morphia.test.query.filters.TestGt.testMatchQuantities",
            ),
            findings(inventory(owned), inventory(owned), auditLog = log).map { it.key },
        )
    }

    @Test
    fun aBuildThatRunsNoTestIsAFinding() {
        assertEquals(
            listOf("FAILING_AUDIT:build"),
            findings(
                    inventory(owned),
                    inventory(owned),
                    auditLog = "[DISCOVERY] critter: the operator tests exited non-zero",
                )
                .map { it.key },
        )
    }

    @Test
    fun aStaleOverrideIsAFinding() {
        val extraction =
            """
            Wrote 0 scenarios
            NOT EXTRACTED query/filters/gt "Match Prices": its source changed, and find modifiers
            NOT EXTRACTED query/filters/gt "Match Prices": its source changed, and data syntax
            NOT EXTRACTED query/filters/gt "Match Prices": the override matches no example on the page
            NOT EXTRACTED query/filters/gt "Other": find modifiers
            """
                .trimIndent()
        assertEquals(
            listOf(
                "FAILING_AUDIT:extraction:refresh:query/filters/gt \"Match Prices\"",
                "FAILING_AUDIT:extraction:stale-override:query/filters/gt \"Match Prices\"",
            ),
            findings(inventory(owned), inventory(owned), extraction = extraction).map { it.key },
        )
    }

    @Test
    fun missingApiAndScenariosHaveStableKeys() {
        val found =
            findings(
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
                listOf(
                    MissingScenario(
                        "query/filters/gt/\$exists: false",
                        "query/filters/gt \"\$exists: false\": UNSUPPORTED (SYNTAX)",
                    )
                ),
            )
        assertEquals(
            listOf(
                "MISSING_API:aggregation/expressions/\$sigmoid",
                "MISSING_SCENARIO:query/filters/gt/\$exists: false",
            ),
            found.map { it.key },
        )
        assertTrue(found[0].body.contains("aggregation/expressions/sigmoid/applySigmoid"))
    }

    @Test
    fun passingTestsFileNothing() {
        assertEquals(
            emptyList<Any>(),
            findings(
                inventory(owned),
                inventory(owned),
                auditLog = "[INFO] Tests run: 3, Failures: 0",
            ),
        )
    }
}
