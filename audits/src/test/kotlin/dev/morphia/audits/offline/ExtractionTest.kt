package dev.morphia.audits.offline

import com.fasterxml.jackson.databind.ObjectMapper
import dev.morphia.audits.extraction.BlockRole
import dev.morphia.audits.extraction.Coverage
import dev.morphia.audits.extraction.Documented
import dev.morphia.audits.extraction.DocumentedExample
import dev.morphia.audits.extraction.DocumentedPage
import dev.morphia.audits.extraction.ExampleInventory
import dev.morphia.audits.extraction.ExampleOverride
import dev.morphia.audits.extraction.ExampleOverrides
import dev.morphia.audits.extraction.ExampleStatus
import dev.morphia.audits.extraction.Extraction
import dev.morphia.audits.extraction.FormSource
import dev.morphia.audits.extraction.Overrides
import dev.morphia.audits.extraction.ScenarioPatch
import dev.morphia.audits.extraction.SourceBlock
import java.nio.file.Files
import java.nio.file.Path
import org.bson.BsonDocument
import org.bson.BsonString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ExtractionTest {
    @TempDir lateinit var repository: Path

    private fun block(role: BlockRole, text: String) =
        SourceBlock(role, text, Documented.sha256(text.toByteArray()))

    private val blocks =
        listOf(
            block(BlockRole.DATA, "db.items.insertMany([{ _id: 1, qty: 5 }])"),
            block(BlockRole.ACTION, "db.items.find({ qty: { \$gt: 4 } })"),
            block(BlockRole.EXPECTED, "[ { _id: 1, qty: 5 } ]"),
        )

    private fun inventory(
        title: String = "Match Greater Quantities",
        stem: String = "matchGreaterQuantities",
    ) =
        ExampleInventory(
            sourceRepository = "mongodb/docs",
            commit = "a".repeat(40),
            pages =
                listOf(
                    DocumentedPage(
                        "query/filters",
                        "gt",
                        FormSource(
                            "a".repeat(40),
                            "content/manual/manual/source/reference/operator/query/gt.txt",
                        ),
                        "b".repeat(40),
                        emptyList(),
                        listOf(
                            DocumentedExample(
                                title,
                                ExampleStatus.UNOWNED,
                                stem = stem,
                                blocks = blocks,
                            )
                        ),
                    )
                ),
            skipped = emptyList(),
            summary = emptyMap(),
        )

    private val path =
        "core/src/test/resources/dev/morphia/test/query/filters/gt/matchGreaterQuantities/scenario.json"

    @Test
    fun anUnownedExampleGetsItsScenario() {
        val result = Extraction.extract(repository, inventory(), ExampleOverrides())
        assertEquals(emptyList<Any>(), result.failures)
        val scenario = BsonDocument.parse(result.written.getValue(path))
        assertEquals("query/filters/gt/matchGreaterQuantities", scenario.getString("name").value)
        assertEquals("Match Greater Quantities", scenario.getString("title").value)
        assertEquals("FILTER", scenario.getString("context").value)
        assertEquals(
            BsonDocument.parse("{qty: {\$gt: 4}}"),
            scenario.getDocument("action").getDocument("filter"),
        )
        assertEquals("UNORDERED", scenario.getDocument("comparison").getString("order").value)
        assertEquals(ExampleStatus.OWNED, result.inventory.pages[0].examples[0].status)
    }

    @Test
    fun anOverrideAppliesToItsExample() {
        val override =
            ExampleOverride(
                "query/filters/gt",
                "Match Greater Quantities",
                Overrides.identity(blocks),
                "the page lists the results in _id order, which the sort it doesn't show defines",
                order = "ORDERED",
                minimumServer = "7.0.0",
            )
        val result =
            Extraction.extract(
                repository,
                inventory(),
                ExampleOverrides(overrides = listOf(override)),
            )
        val scenario = BsonDocument.parse(result.written.getValue(path))
        assertEquals("ORDERED", scenario.getDocument("comparison").getString("order").value)
        assertEquals("7.0.0", scenario.getDocument("requirements").getString("minimumServer").value)
    }

    @Test
    fun anOverrideForAChangedExampleIsStale() {
        val override =
            ExampleOverride(
                "query/filters/gt",
                "Match Greater Quantities",
                "0".repeat(64),
                "reason",
                exclude = true,
            )
        val result =
            Extraction.extract(
                repository,
                inventory(),
                ExampleOverrides(overrides = listOf(override)),
            )
        assertEquals(
            listOf("the override matches no example on the page"),
            result.failures.map { it.detail },
        )
        // The changed example isn't excluded by a stale override, so its scenario is written.
        assertTrue(path in result.written)
    }

    @Test
    fun aRetitledExampleKeepsItsScenario() {
        val first = Extraction.extract(repository, inventory(), ExampleOverrides())
        Files.createDirectories(repository.resolve(path).parent)
        Files.writeString(repository.resolve(path), first.written.getValue(path))
        val retitled =
            Extraction.extract(
                repository,
                inventory("Find Quantities Above Four", "findQuantitiesAboveFour"),
                ExampleOverrides(),
            )
        assertEquals(emptyMap<String, String>(), retitled.written)
        assertTrue(
            retitled.failures.single().detail.contains("the page retitled it"),
            retitled.failures.toString(),
        )
    }

    @Test
    fun aCommittedScenarioIsNeverRewritten() {
        Files.createDirectories(repository.resolve(path).parent)
        Files.writeString(repository.resolve(path), "{}")
        val result = Extraction.extract(repository, inventory(), ExampleOverrides())
        assertEquals(emptyMap<String, String>(), result.written)
        assertTrue(result.failures.single().detail.contains("exists"), result.failures.toString())
    }

    @Test
    fun aPipelineNamingAnotherDatabaseIsRefused() {
        val crossDatabase =
            listOf(
                block(BlockRole.DATA, "db.items.insertMany([{ _id: 1 }])"),
                block(
                    BlockRole.ACTION,
                    "db.items.aggregate([{ \$out: { db: \"reporting\", coll: \"copy\" } }])",
                ),
            )
        val page = inventory().pages[0]
        val result =
            Extraction.extract(
                repository,
                inventory()
                    .copy(
                        pages =
                            listOf(
                                page.copy(
                                    examples = listOf(page.examples[0].copy(blocks = crossDatabase))
                                )
                            )
                    ),
                ExampleOverrides(),
            )
        assertEquals(
            listOf("a stage naming another database: \$out"),
            result.failures.map { it.detail },
        )
    }

    @Test
    fun anOverrideOnAnUnreadableExampleIsReported() {
        val page = inventory().pages[0]
        val unsupported =
            page.examples[0].copy(status = ExampleStatus.UNSUPPORTED, detail = "SYNTAX")
        val override =
            ExampleOverride(
                "query/filters/gt",
                unsupported.title,
                Overrides.identity(blocks),
                "reason",
                order = "ORDERED",
            )
        val result =
            Extraction.extract(
                repository,
                inventory().copy(pages = listOf(page.copy(examples = listOf(unsupported)))),
                ExampleOverrides(overrides = listOf(override)),
            )
        assertEquals(
            listOf("the override can't apply: the example is UNSUPPORTED"),
            result.failures.map { it.detail },
        )
    }

    @Test
    fun anOwnedExampleNeedsItsScenario() {
        val page = inventory().pages[0]
        val owned =
            page.examples[0].copy(
                status = ExampleStatus.OWNED,
                owner = "query/filters/gt/matchGreaterQuantities",
            )
        val inventory = inventory().copy(pages = listOf(page.copy(examples = listOf(owned))))
        assertEquals(1, Coverage.missingScenarios(repository, inventory, ExampleOverrides()).size)
        Files.createDirectories(repository.resolve(path).parent)
        Files.writeString(repository.resolve(path), "{}")
        assertEquals(
            emptyList<String>(),
            Coverage.missingScenarios(repository, inventory, ExampleOverrides()),
        )
    }

    @Test
    fun aPatchThatNamesAnotherDatabaseIsRefused() {
        val pipeline =
            listOf(
                block(BlockRole.DATA, "db.items.insertMany([{ _id: 1 }])"),
                block(BlockRole.ACTION, "db.items.aggregate([{ \$out: \"copy\" }])"),
            )
        val page = inventory().pages[0]
        val override =
            ExampleOverride(
                "query/filters/gt",
                page.examples[0].title,
                Overrides.identity(pipeline),
                "reason",
                patches =
                    listOf(
                        ScenarioPatch(
                            "/action/pipeline/0/\$out",
                            Overrides.digest(BsonString("copy")),
                            ObjectMapper().readTree("""{"db": "reporting", "coll": "copy"}"""),
                        )
                    ),
            )
        val result =
            Extraction.extract(
                repository,
                inventory()
                    .copy(
                        pages =
                            listOf(
                                page.copy(
                                    examples = listOf(page.examples[0].copy(blocks = pipeline))
                                )
                            )
                    ),
                ExampleOverrides(overrides = listOf(override)),
            )
        assertEquals(
            listOf("a stage naming another database: \$out"),
            result.failures.map { it.detail },
        )
    }

    @Test
    fun aPageTheReaderCouldNotReadFailsTheAudit() {
        val page =
            inventory()
                .pages[0]
                .copy(
                    examples = emptyList(),
                    problems = listOf("MISSING_SECTION: no examples section"),
                )
        val missing =
            Coverage.missingScenarios(
                repository,
                inventory().copy(pages = listOf(page)),
                ExampleOverrides(),
            )
        assertEquals(
            listOf(
                "query/filters/gt: the reader couldn't read the page: MISSING_SECTION: no examples section"
            ),
            missing,
        )
    }

    @Test
    fun aChangedExampleIsRefreshedAndKeepsItsReviewedFields() {
        val first = Extraction.extract(repository, inventory(), ExampleOverrides())
        val reviewed =
            BsonDocument.parse(first.written.getValue(path)).apply {
                getDocument("comparison")["order"] = BsonString("ORDERED")
            }
        Files.createDirectories(repository.resolve(path).parent)
        Files.writeString(repository.resolve(path), reviewed.toJson())
        val committed = first.inventory
        val page = committed.pages[0]
        val changed =
            listOf(
                block(BlockRole.DATA, "db.items.insertMany([{ _id: 1, qty: 9 }])"),
                block(BlockRole.ACTION, "db.items.find({ qty: { \$gt: 8 } })"),
                block(BlockRole.EXPECTED, "[ { _id: 1, qty: 9 } ]"),
            )
        val current =
            committed.copy(
                pages =
                    listOf(page.copy(examples = listOf(page.examples[0].copy(blocks = changed))))
            )
        val refreshed = Extraction.refresh(repository, current, committed, ExampleOverrides())
        val scenario = BsonDocument.parse(refreshed.written.getValue(path))
        assertEquals(
            BsonDocument.parse("{qty: {\$gt: 8}}"),
            scenario.getDocument("action").getDocument("filter"),
        )
        assertEquals("ORDERED", scenario.getDocument("comparison").getString("order").value)
        // An unchanged example isn't rewritten.
        assertEquals(
            emptyMap<String, String>(),
            Extraction.refresh(repository, committed, committed, ExampleOverrides()).written,
        )
    }

    @Test
    fun anExampleWhoseCodeVanishedIsAFailure() {
        val committed = Extraction.extract(repository, inventory(), ExampleOverrides()).inventory
        val page = committed.pages[0]
        val current =
            committed.copy(
                pages =
                    listOf(
                        page.copy(examples = listOf(page.examples[0].copy(blocks = emptyList())))
                    )
            )
        val refreshed = Extraction.refresh(repository, current, committed, ExampleOverrides())
        assertEquals(
            listOf("its source changed, and the reader no longer finds the example's code"),
            refreshed.failures.map { it.detail },
        )
    }
}
