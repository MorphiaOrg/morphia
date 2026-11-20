package dev.morphia.audits.offline

import dev.morphia.audits.CorpusInventory
import dev.morphia.audits.bindings.BindingPolicy
import dev.morphia.audits.bindings.LegacyBindings
import dev.morphia.audits.bindings.ManualMethod
import dev.morphia.audits.bindings.NameBackport
import dev.morphia.audits.bindings.NameBackportReview
import dev.morphia.audits.bindings.PageTitle
import dev.morphia.audits.bindings.ReviewedStem
import dev.morphia.audits.bindings.TitleSource
import dev.morphia.audits.bindings.UnboundResource
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class NameBackportTest {
    @TempDir lateinit var repository: Path

    private val type = "dev.morphia.test.query.filters.TestEq"
    private val source
        get() = repository.resolve("core/src/test/java/dev/morphia/test/query/filters/TestEq.java")

    @BeforeEach
    fun initialize() {
        LegacyBindings.packages.forEach {
            repository.resolve("core/src/test/resources/dev/morphia/test/$it").createDirectories()
            repository.resolve("core/src/test/java/dev/morphia/test/$it").createDirectories()
        }
        listOf("TemplatedTestBase", "TestBase").forEach {
            repository
                .resolve("core/src/test/java/dev/morphia/test/$it.java")
                .writeText(
                    "public class $it {\n    public void testPipeline(Object o) {\n    }\n}\n"
                )
        }
    }

    private fun resource(path: String, content: String = "[]") =
        repository.resolve("core/src/test/resources/dev/morphia/test/$path").also {
            it.parent.createDirectories()
            it.writeText(content)
        }

    private fun eq(body: String) =
        source.writeText(
            """
            package dev.morphia.test.query.filters;

            public class TestEq extends TemplatedTestBase {
            $body
            }
            """
                .trimIndent()
        )

    private fun review(
        pages: List<PageTitle> = emptyList(),
        stems: List<ReviewedStem> = emptyList(),
    ) = NameBackportReview(1, pages, stems)

    private fun plan(
        review: NameBackportReview,
        policy: BindingPolicy = BindingPolicy(1, emptyList(), emptyList()),
    ) =
        NameBackport.plan(
            LegacyBindings.capture(repository, "baseline", policy),
            CorpusInventory.capture(repository, "baseline"),
            review,
            NameBackport.inheritedMethods(repository),
        )

    @Test
    fun plainTitlesDropLegacyMarkup() {
        assertEquals(
            "Use \$percentile in a \$project Stage",
            NameBackport.plainTitle("Use |operatorName| in a ``\$project`` Stage", "percentile"),
        )
        assertEquals(
            "Remove With bulkWrite()",
            NameBackport.plainTitle("Remove With :method:`~db.collection.bulkWrite()`", "pull"),
        )
        assertEquals(
            "Alternative to \$where",
            NameBackport.plainTitle("Example 2: Alternative to ``\$where``", "where"),
        )
        assertEquals(
            "\$meta: \"indexKey\" :: Aggregation",
            NameBackport.plainTitle("``\$meta: \"indexKey\"`` :: Aggregation [1]", "meta"),
        )
    }

    @Test
    fun genericSectionLabelsAreDropped() {
        assertEquals(
            "Tangent of Value in Degrees",
            NameBackport.plainTitle("main :: Tangent of Value in Degrees", "tan"),
        )
        assertEquals(
            "Inactive Sessions  :: Replica Set",
            NameBackport.plainTitle("Inactive Sessions  :: Replica Set", "x"),
        )
    }

    @Test
    fun renamesMethodsFixturesAndJavadocTogetherPreservingBytes() {
        resource("query/filters/eq/example1/name", "Match an ``Embedded`` Document\n")
        resource("query/filters/eq/example1/action.json", "[ { \$match: { a: 1 } } ]")
        resource("query/filters/eq/example2/name", "main\n")
        resource("query/filters/eq/example2/lock", "keep\n")
        resource("query/filters/eq/example2/action.json")
        resource("query/filters/eq/example3/name", "Watch\n")
        eq(
            """
            /**
             * test data: dev/morphia/test/query/filters/eq/example7
             */
            @Test
            @DisplayName("Match an ``Embedded`` Document")
            public void testExample1() { testQuery(q -> q); }

            @Test
            public void testExample2() { }

            @Test
            public void testExample4() { }
            """
        )
        val policy =
            BindingPolicy(
                1,
                listOf(UnboundResource("query/filters/eq/example3", "marker only")),
                listOf(ManualMethod(type, "testExample4", "placeholder")),
            )
        val corpus = CorpusInventory.capture(repository, "baseline")
        val before = LegacyBindings.capture(repository, "baseline", policy)
        val ledger =
            plan(
                review(
                    listOf(
                        PageTitle("query/filters/eq", "\$eq (query predicate operator)", "pinned")
                    ),
                    listOf(ReviewedStem("query/filters/eq/example4", "eqPlaceholder", "no fixture")),
                ),
                policy,
            )

        val renames = ledger.renames.associateBy { it.oldMethod ?: it.oldResource }
        assertEquals("testMatchAnEmbeddedDocument", renames.getValue("testExample1").newMethod)
        assertEquals("testEqQueryPredicateOperator", renames.getValue("testExample2").newMethod)
        assertEquals(TitleSource.PAGE_TITLE, renames.getValue("testExample2").titleSource)
        assertEquals(
            "query/filters/eq/watch",
            renames.getValue("query/filters/eq/example3").newResource,
        )
        assertNull(renames.getValue("testExample4").newResource)
        assertEquals(TitleSource.REVIEWED, renames.getValue("testExample4").titleSource)

        NameBackport.apply(repository, ledger)
        NameBackport.verify(repository, corpus, ledger)

        val text = source.readText()
        assertTrue(
            "test data: dev/morphia/test/query/filters/eq/matchAnEmbeddedDocument" in text,
            text,
        )
        assertTrue("public void testEqPlaceholder()" in text, text)
        assertTrue("@DisplayName(\"Match an ``Embedded`` Document\")" in text, text)
        assertFalse(Regex("example[0-9]|Example[0-9]").containsMatchIn(text), text)
        val renamedPolicy = NameBackport.project(policy, ledger)
        assertEquals(
            NameBackport.project(before, ledger, "after"),
            LegacyBindings.capture(repository, "after", renamedPolicy),
        )
        assertEquals("query/filters/eq/watch", renamedPolicy.unboundResources.single().resource)
        assertEquals("testEqPlaceholder", renamedPolicy.manualMethods.single().method)
        assertEquals(
            "keep\n",
            repository
                .resolve(
                    "core/src/test/resources/dev/morphia/test/query/filters/eq/eqQueryPredicateOperator/lock"
                )
                .readText(),
        )
    }

    @Test
    fun rejectsCollisionsGenericTitlesAndStaleReview() {
        resource("query/filters/eq/example1/name", "Same Title\n")
        resource("query/filters/eq/example1/action.json")
        resource("query/filters/eq/example2/name", "Same title\n")
        resource("query/filters/eq/example2/action.json")
        resource("query/filters/eq/example3/name", "main\n")
        resource("query/filters/eq/example3/action.json")
        resource("query/filters/eq/example4/name", "Pipeline\n")
        resource("query/filters/eq/example4/action.json")
        eq((1..4).joinToString("\n") { "@Test public void testExample$it() { }" })

        val message =
            assertThrows(IllegalArgumentException::class.java) {
                    plan(
                        review(
                            stems = listOf(ReviewedStem("query/filters/eq/gone", "gone", "stale"))
                        )
                    )
                }
                .message!!

        assertTrue("Fixture name collision" in message, message)
        assertTrue("Generic title for query/filters/eq/example3" in message, message)
        assertTrue("$type#testPipeline would hide an inherited method" in message, message)
        assertTrue("Unused reviewed stem for query/filters/eq/gone" in message, message)
    }

    @Test
    fun verifyRejectsEveryKindOfBrokenMigration() {
        resource("query/filters/eq/example1/name", "First Case\n")
        resource("query/filters/eq/example1/action.json", "[1]")
        resource("query/filters/eq/example2/name", "Second Case\n")
        resource("query/filters/eq/example2/action.json", "[2]")
        eq(
            """
            /**
             * test data: dev/morphia/test/query/filters/eq/example1
             */
            @Test
            public void testExample1() { }

            /**
             * test data: dev/morphia/test/query/filters/eq/example2
             */
            @Test
            public void testExample2() { }
            """
        )
        val corpus = CorpusInventory.capture(repository, "baseline")
        val ledger = plan(review())
        NameBackport.apply(repository, ledger)
        NameBackport.verify(repository, corpus, ledger)
        val fixtures =
            repository.resolve("core/src/test/resources/dev/morphia/test/query/filters/eq")
        val migrated = source.readText()

        fun rejects(message: String, mutate: () -> Unit, restore: () -> Unit) {
            mutate()
            val failure =
                assertThrows(IllegalArgumentException::class.java) {
                    NameBackport.verify(repository, corpus, ledger)
                }
            assertTrue(message in failure.message!!, failure.message)
            restore()
            NameBackport.verify(repository, corpus, ledger)
        }

        val action = fixtures.resolve("firstCase/action.json")
        rejects("Fixture bytes changed", { action.writeText("[3]") }, { action.writeText("[1]") })
        rejects(
            "Missing preserved fixture file",
            { Files.delete(action) },
            { action.writeText("[1]") },
        )
        val legacy = fixtures.resolve("example1")
        rejects(
            "Legacy fixture directory still present",
            { legacy.createDirectories() },
            { Files.delete(legacy) },
        )
        rejects(
            "Ordinal name remains",
            {
                source.writeText(
                    migrated.replace("public void testFirstCase", "public void testExample1")
                )
            },
            { source.writeText(migrated) },
        )
        rejects(
            "testFirstCase documents dev/morphia/test/query/filters/eq/secondCase",
            { source.writeText(migrated.replaceFirst("eq/firstCase", "eq/secondCase")) },
            { source.writeText(migrated) },
        )
        val lock = fixtures.resolve("firstCase/lock")
        lock.writeText("keep\n")
        val locked = CorpusInventory.capture(repository, "locked")
        val digest =
            locked.examples.flatMap { it.files }.single { it.path.endsWith("/lock") }.sha256
        val retired =
            mapOf(
                "core/src/test/resources/dev/morphia/test/query/filters/eq/firstCase/lock" to digest
            )
        val stillThere =
            assertThrows(IllegalArgumentException::class.java) {
                NameBackport.verify(repository, locked, ledger, retired)
            }
        assertTrue("Retired file is still present" in stillThere.message!!, stillThere.message)
        Files.delete(lock)
        NameBackport.verify(repository, locked, ledger, retired)
        val wrong =
            assertThrows(IllegalArgumentException::class.java) {
                NameBackport.verify(repository, locked, ledger, mapOf(retired.keys.single() to "0"))
            }
        assertTrue("Retired file differs" in wrong.message!!, wrong.message)
        rejects(
            "Unparsed test data Javadoc",
            { source.writeText(migrated.replaceFirst("eq/firstCase", "eq/firstCase see a/b")) },
            { source.writeText(migrated) },
        )
    }

    @Test
    fun applyLeavesTheTreeUntouchedWhenADestinationExists() {
        resource("query/filters/eq/example1/name", "Taken\n")
        resource("query/filters/eq/example1/action.json")
        eq("@Test public void testExample1() { }")
        val ledger = plan(review())
        resource("query/filters/eq/taken/action.json")
        val before = source.readText()

        assertThrows(IllegalArgumentException::class.java) {
            NameBackport.apply(repository, ledger)
        }

        assertEquals(before, source.readText())
        assertTrue(
            repository
                .resolve("core/src/test/resources/dev/morphia/test/query/filters/eq/example1")
                .exists()
        )
        assertTrue(
            Files.isDirectory(
                repository.resolve(
                    "core/src/test/resources/dev/morphia/test/query/filters/eq/taken"
                )
            )
        )
    }
}
