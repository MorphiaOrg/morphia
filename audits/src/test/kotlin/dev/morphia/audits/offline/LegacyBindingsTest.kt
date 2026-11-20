package dev.morphia.audits.offline

import dev.morphia.audits.bindings.BindingDisposition
import dev.morphia.audits.bindings.BindingPolicy
import dev.morphia.audits.bindings.LegacyBindings
import dev.morphia.audits.bindings.ManualMethod
import dev.morphia.audits.bindings.UnboundResource
import dev.morphia.audits.coverage.AuditJson
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LegacyBindingsTest {
    @TempDir lateinit var repository: Path

    private val none = BindingPolicy(1, emptyList(), emptyList())

    @BeforeEach
    fun initialize() {
        LegacyBindings.packages.forEach {
            repository.resolve("core/src/test/resources/dev/morphia/test/$it").createDirectories()
            repository.resolve("core/src/test/java/dev/morphia/test/$it").createDirectories()
        }
    }

    private fun resource(path: String, content: String = "[]") =
        repository.resolve("core/src/test/resources/dev/morphia/test/$path").also {
            it.parent.createDirectories()
            it.writeText(content)
        }

    private fun source(
        category: String,
        name: String,
        body: String,
        parent: String = "TemplatedTestBase",
    ) =
        repository
            .resolve("core/src/test/java/dev/morphia/test/$category/$name.java")
            .writeText(
                """
                package dev.morphia.test.${category.replace('/', '.')};

                public class $name extends $parent {
                $body
                }
                """
                    .trimIndent()
            )

    private fun failure(policy: BindingPolicy = none): String =
        assertThrows(IllegalArgumentException::class.java) {
                LegacyBindings.capture(repository, "baseline", policy)
            }
            .message!!

    @Test
    fun recordsBindingsAndDeclaredEvidenceWithoutChangingSources() {
        resource("query/filters/eq/example1/action.json")
        resource("query/filters/eq/calculateTotals/action.json")
        resource("query/filters/eq/watch/name", "Watch")
        resource("aggregation/stages/collStats/ignored", "tested elsewhere")
        source(
            "query/filters",
            "TestEq",
            """
            @Test
            @DisplayName("Equals a ``value``")
            public void testExample1() { testQuery(q -> q); }

            @Test
            @Disabled
            public void testCalculateTotals() { }

            @Test
            public void testHandwritten() { assertTrue(true); }

            public void helper() { }
            """,
        )
        source(
            "aggregation/stages",
            "TestCollectionStats",
            "@Test public void testStats() { }",
            "TestBase",
        )
        val policy =
            BindingPolicy(
                1,
                listOf(UnboundResource("query/filters/eq/watch", "marker only")),
                listOf(
                    ManualMethod(
                        "dev.morphia.test.query.filters.TestEq",
                        "testHandwritten",
                        "no fixture",
                    )
                ),
            )
        val before = snapshot()

        val catalog = LegacyBindings.capture(repository, "baseline", policy)

        assertEquals(before, snapshot())
        val methods = catalog.methods.associateBy { it.method }
        assertEquals(
            setOf("testExample1", "testCalculateTotals", "testHandwritten", "testStats"),
            methods.keys,
        )
        with(methods.getValue("testExample1")) {
            assertEquals(BindingDisposition.BOUND, disposition)
            assertEquals("query/filters/eq/example1", resource)
            assertEquals("Equals a ``value``", displayName)
            assertFalse(disabled || emptyBody)
            assertEquals(64, bodySha256.length)
            assertEquals("core/src/test/java/dev/morphia/test/query/filters/TestEq.java", source)
        }
        with(methods.getValue("testCalculateTotals")) {
            assertEquals(BindingDisposition.BOUND, disposition)
            assertTrue(disabled && emptyBody)
            assertNull(displayName)
        }
        assertEquals(BindingDisposition.MANUAL, methods.getValue("testHandwritten").disposition)
        assertEquals("no fixture", methods.getValue("testHandwritten").reason)
        assertEquals(BindingDisposition.INDEPENDENT, methods.getValue("testStats").disposition)

        val descriptor = catalog.descriptor()
        val eq = descriptor.classes.single()
        assertEquals("dev.morphia.test.query.filters.TestEq", eq.type)
        assertEquals("dev/morphia/test/query/filters/eq", eq.resources)
        assertEquals(listOf("calculateTotals", "example1"), eq.bindings.map { it.resource })
        assertEquals(listOf("testHandwritten"), eq.manual.map { it.method })
        assertEquals(listOf("watch"), eq.unbound.map { it.resource })
        assertTrue("dev.morphia.test.aggregation.stages" in descriptor.packages)
        assertEquals(catalog, AuditJson.legacyBindings(AuditJson.write(catalog)))
        assertEquals(descriptor, AuditJson.bindingDescriptor(AuditJson.write(descriptor)))
    }

    @Test
    fun rejectsUnownedMethodsAndFixtures() {
        resource("query/filters/eq/example1/action.json")
        resource("query/filters/eq/example2/action.json")
        resource("query/filters/orphan/example1/action.json")
        resource("query/filters/ignoredButPopulated/ignored", "reason")
        resource("query/filters/ignoredButPopulated/example1/action.json")
        source(
            "query/filters",
            "TestEq",
            "@Test public void testExample1() { } @Test public void testExample3() { }",
        )

        val message = failure()

        assertTrue(
            "Unowned test method without a fixture: dev.morphia.test.query.filters.TestEq#testExample3" in
                message,
            message,
        )
        assertTrue("Fixture without a test method: query/filters/eq/example2" in message, message)
        assertTrue("No test class owns query/filters/orphan" in message, message)
        assertTrue(
            "No test owns fixture query/filters/ignoredButPopulated/example1" in message,
            message,
        )
    }

    @Test
    fun rejectsStaleOrIncompletePolicy() {
        resource("query/filters/eq/example1/action.json")
        source("query/filters", "TestEq", "@Test public void testExample1() { }")
        val type = "dev.morphia.test.query.filters.TestEq"

        val message =
            failure(
                BindingPolicy(
                    1,
                    listOf(
                        UnboundResource("query/filters/eq/example1", "now bound"),
                        UnboundResource("query/filters/eq/removed", "gone"),
                    ),
                    listOf(
                        ManualMethod(type, "testExample1", "now bound"),
                        ManualMethod(type, "testRemoved", "gone"),
                    ),
                )
            )

        assertTrue(
            "Stale unbound policy for bound fixture query/filters/eq/example1" in message,
            message,
        )
        assertTrue("Stale unbound policy for query/filters/eq/removed" in message, message)
        assertTrue("Stale manual policy for bound $type#testExample1" in message, message)
        assertTrue("Stale manual policy for missing $type#testRemoved" in message, message)
    }

    @Test
    fun rejectsMissingFixtureDirectoryAndComputedDisplayNames() {
        source("query/filters", "TestEq", "@Test public void testOnly() { }")
        assertTrue(
            "Missing fixture directory for templated class: query/filters/eq" in
                failure(
                    BindingPolicy(
                        1,
                        emptyList(),
                        listOf(
                            ManualMethod(
                                "dev.morphia.test.query.filters.TestEq",
                                "testOnly",
                                "manual",
                            )
                        ),
                    )
                )
        )

        resource("query/filters/eq/example1/action.json")
        source(
            "query/filters",
            "TestEq",
            "static final String N = \"x\"; @Test @DisplayName(N) public void testExample1() { }",
        )
        assertTrue("must be a string literal" in failure())
    }

    @Test
    fun mirrorsTheTemplateLoaderNamingConvention() {
        assertEquals("example1", LegacyBindings.derived("testExample1"))
        assertEquals("isoWeek", LegacyBindings.derived("testIsoWeek"))
        assertEquals(
            "dev/morphia/test/query/filters/eq",
            dev.morphia.audits.bindings.resources("dev.morphia.test.query.filters.TestEq"),
        )
    }

    @Test
    fun rejectsUnboundFixturesWithExecutableFiles() {
        resource("query/filters/eq/example1/action.json")
        resource("query/filters/eq/watch/name", "Watch")
        resource("query/filters/eq/watch/expected.json")
        source("query/filters", "TestEq", "@Test public void testExample1() { }")

        val message =
            failure(
                BindingPolicy(
                    1,
                    listOf(UnboundResource("query/filters/eq/watch", "marker")),
                    emptyList(),
                )
            )

        assertTrue(
            "Unbound fixture query/filters/eq/watch holds more than markers" in message,
            message,
        )
    }

    /**
     * The committed descriptor must be exactly what the reviewed policy produces from the sources.
     */
    @Test
    fun committedDescriptorMatchesTheReviewedPolicy() {
        val root =
            generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isDirectory(it.resolve("core/src/test/java/dev/morphia/test")) }
        fun read(path: String) =
            Files.readAllBytes(root.resolve(path)).decodeToString(throwOnInvalidSequence = true)
        val committed = AuditJson.legacyBindings(read("audits/catalog/legacy-bindings.json"))
        val catalog =
            LegacyBindings.capture(
                root,
                committed.repositoryRevision,
                AuditJson.bindingPolicy(read("audits/catalog/binding-policy.json")),
            )

        assertEquals(committed, catalog)
        assertEquals(
            read("core/src/test/resources/dev/morphia/test/example-bindings.json"),
            AuditJson.write(catalog.descriptor()),
        )
    }

    private fun snapshot(): Map<Path, List<Byte>> =
        Files.walk(repository).use { paths ->
            paths
                .filter { Files.isRegularFile(it) }
                .toList()
                .associateWith { Files.readAllBytes(it).toList() }
        }
}
