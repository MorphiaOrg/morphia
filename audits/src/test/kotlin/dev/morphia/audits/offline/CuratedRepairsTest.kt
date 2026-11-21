package dev.morphia.audits.offline

import dev.morphia.audits.bindings.BindingDescriptor
import dev.morphia.audits.bindings.DescriptorBinding
import dev.morphia.audits.bindings.DescriptorClass
import dev.morphia.audits.bindings.DescriptorUnbound
import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.repairs.AssertionPath
import dev.morphia.audits.repairs.CuratedRepairs
import dev.morphia.audits.repairs.OptionCall
import dev.morphia.audits.repairs.RepairCategory
import dev.morphia.audits.repairs.RepairDisposition
import dev.morphia.audits.repairs.RepairReview
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CuratedRepairsTest {
    @TempDir lateinit var repository: Path

    private val categories =
        listOf(
            RepairCategory("MORPHIA_FORM", "equivalent form", "equivalence rule"),
            RepairCategory("EXTRACTION", "parser artifact", "native extraction"),
        )
    private val type = "dev.morphia.test.query.filters.TestEq"

    private fun resource(path: String, content: String = "[]") =
        repository.resolve("core/src/test/resources/dev/morphia/test/query/filters/eq/$path").also {
            it.parent.createDirectories()
            it.writeText(content)
        }

    private fun source(body: String) =
        repository
            .resolve("core/src/test/java/dev/morphia/test/query/filters/TestEq.java")
            .also { it.parent.createDirectories() }
            .writeText(
                "package dev.morphia.test.query.filters;\npublic class TestEq extends TemplatedTestBase {\n$body\n}\n"
            )

    private fun descriptor(
        vararg bindings: Pair<String, String>,
        unbound: List<String> = emptyList(),
    ) =
        BindingDescriptor(
            packages = listOf("dev.morphia.test.query.filters"),
            classes =
                listOf(
                    DescriptorClass(
                        type,
                        "dev/morphia/test/query/filters/eq",
                        bindings.map { DescriptorBinding(it.first, it.second) },
                        emptyList(),
                        unbound.map { DescriptorUnbound(it, "marker only") },
                    )
                ),
        )

    private fun review(
        vararg reasons: Pair<String, String>,
        exclusions: Map<String, String> = emptyMap(),
    ) =
        RepairReview(
            1,
            categories,
            mapOf(*reasons),
            listOf(RepairCategory("POLICY_EXCLUSION", "out of scope", "reevaluate on request")),
            exclusions,
        )

    @Test
    fun classifiesLocksAndRecordsOptionsAndAssertionPaths() {
        resource("reordered/lock", "Reorder pipeline\n\nWrong name from parser\n")
        resource("reordered/action.json")
        resource("plain/action.json")
        resource("serializationOnly/action.json")
        resource("empty/action.json")
        resource("custom/data.json")
        resource("disabled/action.json")
        resource("watch/lock", "Reorder pipeline")
        repository
            .resolve(
                "core/src/test/resources/dev/morphia/test/aggregation/stages/collStats/ignored"
            )
            .also { it.parent.createDirectories() }
            .writeText("an admin function\n")
        source(
            """
            @Test public void testReordered() {
                testPipeline(new ActionTestOptions().orderMatters(false).serverVersion("7.0"), a -> a);
            }
            @Test public void testPlain() {
                loadData("items", 2);
                testQuery(q -> q);
            }
            @Test public void testSerializationOnly() {
                testUpdate(new ActionTestOptions().skipDataCheck(true), q -> q);
            }
            @Test public void testEmpty() {
                // testPipeline(a -> a);
            }
            @Test public void testCustom() { assertTrue(loadJson("x", "data", true).isEmpty()); }
            @Test @Disabled public void testDisabled() { testPipeline(a -> a); }
            """
        )
        val before =
            Files.readString(
                repository.resolve("core/src/test/java/dev/morphia/test/query/filters/TestEq.java")
            )

        val report =
            CuratedRepairs.capture(
                repository,
                "baseline",
                descriptor(
                    "testReordered" to "reordered",
                    "testPlain" to "plain",
                    "testSerializationOnly" to "serializationOnly",
                    "testEmpty" to "empty",
                    "testCustom" to "custom",
                    "testDisabled" to "disabled",
                    unbound = listOf("watch"),
                ),
                review(
                    "reorder pipeline" to "MORPHIA_FORM",
                    "wrong name from parser" to "EXTRACTION",
                    exclusions = mapOf("aggregation/stages/collStats" to "POLICY_EXCLUSION"),
                ),
            )

        val examples = report.examples.associateBy { it.fixture.substringAfterLast('/') }
        with(examples.getValue("reordered")) {
            assertEquals(listOf("EXTRACTION", "MORPHIA_FORM"), categories)
            assertEquals(RepairDisposition.PRESERVED_CURATED, disposition)
            assertEquals(
                listOf(OptionCall("orderMatters", "false"), OptionCall("serverVersion", "7.0")),
                options,
            )
            assertEquals(listOf(AssertionPath.SERIALIZATION, AssertionPath.BEHAVIOR), assertions)
            assertEquals(64, markers.single().sha256.length)
        }
        with(examples.getValue("plain")) {
            assertEquals(RepairDisposition.UNCORRECTED, disposition)
            assertEquals(mapOf("data2.json" to "items"), collections)
        }
        assertEquals(emptyMap<String, String>(), examples.getValue("reordered").collections)
        assertEquals(
            listOf(AssertionPath.SERIALIZATION),
            examples.getValue("serializationOnly").assertions,
        )
        assertEquals(emptyList<AssertionPath>(), examples.getValue("empty").assertions)
        assertEquals(listOf(AssertionPath.CUSTOM), examples.getValue("custom").assertions)
        with(examples.getValue("disabled")) {
            assertTrue(disabled)
            assertEquals(emptyList<AssertionPath>(), assertions)
        }
        assertEquals(1, report.summary["assertion.DISABLED"])
        assertEquals(RepairDisposition.NOT_EXECUTED, examples.getValue("watch").disposition)
        assertEquals(1, report.summary["assertion.NONE"])
        assertEquals(
            "an admin function",
            report.exclusions.single { it.folder == "aggregation/stages/collStats" }.reason,
        )
        assertEquals(1, report.summary["exclusion.POLICY_EXCLUSION"])
        assertEquals(2, report.summary["category.MORPHIA_FORM"])
        assertEquals(report, AuditJson.curatedRepairs(AuditJson.write(report)))
        assertEquals(
            before,
            Files.readString(
                repository.resolve("core/src/test/java/dev/morphia/test/query/filters/TestEq.java")
            ),
        )
    }

    @Test
    fun rejectsUnreviewedStaleAndMiscategorizedReasons() {
        resource("reordered/lock", "Reorder pipeline\nSomething new")
        resource("reordered/action.json")
        resource("blank/lock", "  \n")
        resource("blank/action.json")
        source(
            """
            @Test public void testReordered() {
                this.testPipeline(a -> a);
                this.loadData("items", 2);
            }
            @Test public void testBlank() {
                String name = "items";
                loadData(name, 2);
                loadData("items", 3);
                loadData("others", 3);
                boolean flag = true;
                testQuery(new ActionTestOptions().skipDataCheck(flag), q -> q);
                ActionTestOptions options = new ActionTestOptions();
                options.skipActionCheck(true);
            }
            """
        )

        val message =
            assertThrows(IllegalArgumentException::class.java) {
                    CuratedRepairs.capture(
                        repository,
                        "baseline",
                        descriptor("testReordered" to "reordered", "testBlank" to "blank"),
                        review(
                            "reorder pipeline" to "MORPHIA_FORM",
                            "gone" to "EXTRACTION",
                            "odd" to "NOPE",
                            "Not Normalized" to "EXTRACTION",
                            exclusions = mapOf("query/filters/gone" to "POLICY_EXCLUSION"),
                        ),
                    )
                }
                .message!!

        assertTrue(
            "Unreviewed lock reason in dev/morphia/test/query/filters/eq/reordered: something new" in
                message,
            message,
        )
        assertTrue("Stale reviewed lock reason: gone" in message, message)
        assertTrue("$type#testReordered: Unsupported helper call form" in message, message)
        assertTrue("$type#testBlank: Non-literal skipDataCheck argument" in message, message)
        assertTrue(
            "$type#testBlank: Unsupported loadData call form: loadData(name, 2)" in message,
            message,
        )
        assertTrue("$type#testBlank: data3.json is loaded more than once" in message, message)
        assertTrue(
            "$type#testReordered: Unsupported helper call form: this.loadData" in message,
            message,
        )
        assertTrue("$type#testBlank: skipActionCheck must be chained" in message, message)
        assertTrue("Stale reviewed exclusion: query/filters/gone" in message, message)
        assertTrue("Unknown category NOPE" in message, message)
        assertTrue("Reason keys must be trimmed lower case: Not Normalized" in message, message)
        assertTrue(
            "Lock without a reason: dev/morphia/test/query/filters/eq/blank" in message,
            message,
        )
    }

    @Test
    fun retiredLocksKeepTheirCategories() {
        resource("reordered/action.json")
        resource("kept/lock", "Reorder pipeline")
        resource("kept/action.json")
        source(
            """
            @Test public void testReordered() { testPipeline(a -> a); }
            @Test public void testKept() { testPipeline(a -> a); }
            """
        )
        val fixture = "query/filters/eq/reordered"
        val report =
            CuratedRepairs.capture(
                repository,
                "baseline",
                descriptor("testReordered" to "reordered", "testKept" to "kept"),
                review(
                    "reorder pipeline" to "MORPHIA_FORM",
                    "wrong name from parser" to "EXTRACTION",
                ),
                mapOf(fixture to "Reorder pipeline\nWrong name from parser\n"),
            )
        with(report.examples.single { it.fixture == fixture }) {
            assertEquals(RepairDisposition.OVERRIDDEN, disposition)
            assertEquals(listOf("EXTRACTION", "MORPHIA_FORM"), categories)
            assertEquals(emptyList<Any>(), markers)
        }
        assertEquals(1, report.summary["disposition.OVERRIDDEN"])
        assertEquals(1, report.summary["examples.locked"])

        resource("reordered/lock", "Reorder pipeline")
        val message =
            assertThrows(IllegalArgumentException::class.java) {
                    CuratedRepairs.capture(
                        repository,
                        "baseline",
                        descriptor("testReordered" to "reordered", "testKept" to "kept"),
                        review("reorder pipeline" to "MORPHIA_FORM"),
                        mapOf(fixture to "Reorder pipeline", "query/filters/eq/gone" to "x"),
                    )
                }
                .message!!
        assertTrue("Retired lock is still present: $fixture" in message, message)
        assertTrue("Retired lock for an unknown fixture: query/filters/eq/gone" in message, message)
    }

    /**
     * The committed inventory must be what the reviewed classification produces from the sources.
     */
    @Test
    fun committedInventoryMatchesTheSources() {
        val root =
            generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isDirectory(it.resolve("core/src/test/java/dev/morphia/test")) }
        fun read(path: String) =
            Files.readAllBytes(root.resolve(path)).decodeToString(throwOnInvalidSequence = true)
        val committed = AuditJson.curatedRepairs(read("audits/catalog/curated-repairs.json"))

        val report =
            CuratedRepairs.capture(
                root,
                committed.repositoryRevision,
                AuditJson.bindingDescriptor(
                    read("core/src/test/resources/dev/morphia/test/example-bindings.json")
                ),
                AuditJson.repairReview(read("audits/catalog/repair-review.json")),
                AuditJson.formReview(read("audits/catalog/form-review.json"))
                    .declarations
                    .associate { it.fixture to it.lock },
            )

        assertEquals(read("audits/catalog/curated-repairs.json"), AuditJson.write(report))
        assertEquals(113, report.summary["examples.locked"])
        assertEquals(123, report.summary["disposition.OVERRIDDEN"])
        assertEquals(17, report.exclusions.size)
        assertEquals(
            0,
            report.examples.count { it.markers.isNotEmpty() && it.categories.isEmpty() },
        )
    }
}
