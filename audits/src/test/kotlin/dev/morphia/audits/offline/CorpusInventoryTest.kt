package dev.morphia.audits.offline

import com.fasterxml.jackson.databind.json.JsonMapper
import dev.morphia.audits.CorpusInventory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CorpusInventoryTest {
    @TempDir lateinit var repository: Path

    private fun initialize(root: Path = repository) {
        listOf("aggregation/expressions", "aggregation/stages", "query/filters", "query/updates")
            .forEach {
                root.resolve("core/src/test/resources/dev/morphia/test/$it").createDirectories()
                root.resolve("core/src/test/java/dev/morphia/test/$it").createDirectories()
            }
    }

    private fun resource(path: String, content: String): Path =
        repository.resolve("core/src/test/resources/dev/morphia/test/$path").also {
            it.parent.createDirectories()
            it.writeText(content)
        }

    @Test
    fun recordsCuratedFilesAndSourceWithoutChangingThem() {
        initialize()
        resource("query/filters/eq/example1/name", "A corrected title\n")
        resource("query/filters/eq/example1/lock", "Preserve numeric types\n")
        resource("query/filters/eq/example1/lockd", "Legacy misspelled marker")
        resource("query/filters/eq/example1/data1.json", "{ n: NumberLong(2) }")
        resource("query/filters/eq/example1/notes.txt", "Handwritten notes")
        val binary = resource("query/filters/eq/example1/extra.bin", "")
        binary.writeBytes(byteArrayOf(0, -1, 13, 10))
        resource("query/filters/eq/ignored", "Tested elsewhere")
        val source =
            repository.resolve("core/src/test/java/dev/morphia/test/query/filters/TestEq.java")
        source.writeText("// Handwritten body and ActionTestOptions are preserved verbatim\n")
        val before =
            Files.walk(repository).use { paths ->
                paths
                    .filter { Files.isRegularFile(it) }
                    .toList()
                    .associateWith { Files.readAllBytes(it).toList() }
            }

        val inventory = CorpusInventory.capture(repository, "baseline")

        assertEquals("A corrected title", inventory.examples.single().displayName)
        assertEquals(6, inventory.examples.single().files.size)
        assertEquals(setOf("lock", "lockd"), inventory.examples.single().markers.keys)
        assertEquals("Preserve numeric types\n", inventory.examples.single().markers["lock"])
        assertEquals("Tested elsewhere", inventory.exclusions.single().reason)
        assertEquals(1, inventory.testSources.size)
        assertEquals(64, inventory.testSources.single().sha256.length)
        before.forEach { (path, content) ->
            assertEquals(content, Files.readAllBytes(path).toList(), path.toString())
        }
        val after =
            Files.walk(repository).use { paths ->
                paths.filter { Files.isRegularFile(it) }.toList().toSet()
            }
        assertEquals(before.keys, after)
    }

    @Test
    fun includesIncompleteAndDescriptivelyNamedExamples() {
        initialize()
        resource("aggregation/expressions/tsSecond/example2/name", "No action")
        resource("aggregation/expressions/tsSecond/example2/lock", "Incomplete legacy case")
        resource("aggregation/expressions/median/calculateMedianByYear/action.json", "[]")
        repository
            .resolve("core/src/test/resources/dev/morphia/test/query/filters/eq/emptyCase")
            .createDirectories()

        val inventory = CorpusInventory.capture(repository, "baseline")

        assertEquals(3, inventory.examples.size)
        assertTrue(
            inventory.examples.any {
                it.path.endsWith("calculateMedianByYear") && it.displayName == null
            }
        )
        assertTrue(inventory.examples.any { it.path.endsWith("emptyCase") && it.files.isEmpty() })
        assertEquals(
            listOf("lock", "name"),
            inventory.examples
                .single { it.path.endsWith("example2") }
                .files
                .map { Path.of(it.path).fileName.toString() },
        )
    }

    @Test
    fun outputIsDeterministicAndContentChangesAreVisible() {
        initialize()
        val data = resource("query/updates/set/example2/data.json", "{ value: 2 }")
        resource("query/updates/set/example1/name", "First")

        val first = CorpusInventory.capture(repository, "baseline")
        assertEquals(first.toJson(), CorpusInventory.capture(repository, "baseline").toJson())
        val json = JsonMapper().readTree(first.toJson())
        assertEquals(1, json["schemaVersion"].asInt())
        assertEquals("baseline", json["repositoryRevision"].asText())
        assertTrue(first.examples.first().path.endsWith("example1"))

        data.writeText("{ value: 3 }")
        val second = CorpusInventory.capture(repository, "baseline")
        assertNotEquals(
            first.examples.last().files.single().sha256,
            second.examples.last().files.single().sha256,
        )
    }

    @Test
    fun rejectsWrongRepositoryAndMissingCategories() {
        assertThrows(IllegalArgumentException::class.java) {
            CorpusInventory.capture(repository, "baseline")
        }
        initialize()
        Files.delete(repository.resolve("core/src/test/resources/dev/morphia/test/query/updates"))
        assertThrows(IllegalArgumentException::class.java) {
            CorpusInventory.capture(repository, "baseline")
        }
    }

    @Test
    fun inventoryIsIndependentOfCreationOrderAndAbsoluteRoot() {
        val first = repository.resolve("first").createDirectories()
        val second = repository.resolve("second").createDirectories()
        listOf(first, second).forEach { initialize(it) }
        val files =
            listOf(
                "core/src/test/resources/dev/morphia/test/query/filters/eq/readDocuments/name" to
                    "Read Documents",
                "core/src/test/resources/dev/morphia/test/query/filters/eq/readDocuments/action.json" to
                    "{ n: 1 }",
            )
        files.forEach { (path, value) ->
            first.resolve(path).also { it.parent.createDirectories() }.writeText(value)
        }
        files.reversed().forEach { (path, value) ->
            second.resolve(path).also { it.parent.createDirectories() }.writeText(value)
        }
        assertEquals(
            CorpusInventory.capture(first, "baseline").toJson(),
            CorpusInventory.capture(second, "baseline").toJson(),
        )
    }

    @Test
    fun sourceChangesAreVisibleWithoutParsingAwayHandwrittenOptions() {
        initialize()
        val source =
            repository.resolve("core/src/test/java/dev/morphia/test/query/filters/TestEq.java")
        source.writeText("testQuery(new ActionTestOptions().skipDataCheck(true));")
        val before = CorpusInventory.capture(repository, "baseline")
        source.writeText("testQuery(new ActionTestOptions().skipDataCheck(false));")
        val after = CorpusInventory.capture(repository, "baseline")
        assertEquals(before.examples, after.examples)
        assertNotEquals(before.testSources.single().sha256, after.testSources.single().sha256)
    }

    @Test
    fun preservesSharedHelpersSortFixturesAndUnexpectedOperatorFiles() {
        initialize()
        resource("query/sorts/descending/action.json", "{ score: -1 }")
        resource("query/filters/eq/notes.txt", "An operator-level file")
        resource("aggregation/movies.json", "{ title: \"sample\" }")
        val helper =
            repository.resolve("core/src/test/java/dev/morphia/test/TemplatedTestBase.java")
        helper.writeText("// shared handwritten assertions")

        val inventory = CorpusInventory.capture(repository, "baseline")

        assertEquals(3, inventory.additionalResources.size)
        assertTrue(
            inventory.additionalResources.any {
                it.path.endsWith("query/sorts/descending/action.json")
            }
        )
        assertEquals(1, inventory.testSources.size)
        assertTrue(inventory.testSources.single().path.endsWith("TemplatedTestBase.java"))
    }

    @Test
    fun rejectsSymlinkedCorpusEntries() {
        initialize()
        val actual = resource("query/filters/eq/example1/name", "Original")
        Files.createSymbolicLink(actual.parent.resolve("alias"), actual)
        assertThrows(IllegalArgumentException::class.java) {
            CorpusInventory.capture(repository, "baseline")
        }
    }
}
