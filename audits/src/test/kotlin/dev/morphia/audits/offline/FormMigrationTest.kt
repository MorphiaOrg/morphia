package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.repairs.FormMigration
import dev.morphia.audits.repairs.RepairDisposition
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * The MORPHIA_FORM migration is frozen: these tests recheck its committed records. The commands
 * that drafted and captured them read historical pages with the retired RST extractor and are gone.
 */
class FormMigrationTest {
    private val root =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("core/src/test/java/dev/morphia/test")) }

    private fun read(path: String) =
        Files.readAllBytes(root.resolve(path)).decodeToString(throwOnInvalidSequence = true)

    private fun moved() =
        AuditJson.fixtureMigration(read("audits/catalog/fixture-migration.json"))
            .migrations
            .map { it.fixture }
            .toSet()

    /**
     * The committed migration still holds: the report matches its review, every locked example
     * whose only category is MORPHIA_FORM was retired or migrated, and nothing is left to capture.
     */
    @Test
    fun committedMigrationMatchesItsReview() {
        val review = AuditJson.formReview(read("audits/catalog/form-review.json"))
        val report = AuditJson.formMigration(read("audits/catalog/form-migration.json"))
        assertEquals(read("audits/catalog/form-review.json"), AuditJson.write(review))
        val inventory = AuditJson.curatedRepairs(read("audits/catalog/curated-repairs.json"))
        FormMigration.verify(review, report)

        // Every locked example is either retired or kept with its gap, except the untestable
        // change-stream fixtures, which have no test to compare against.
        val formOnly =
            inventory.examples.filter {
                it.categories.isNotEmpty() && "UNTESTABLE" !in it.categories
            }
        // Converted cases keep their legacy test, so the inventory still holds them; migrated
        // cases left it with theirs.
        val known = inventory.examples.map { it.fixture }.toSet()
        val converted = review.migrated.map { it.fixture }.filter { it in known }
        assertEquals(
            formOnly.map { it.fixture }.toSet(),
            (review.declarations.map { it.fixture } +
                    review.unresolved.map { it.fixture } +
                    converted)
                .toSet(),
        )
        assertEquals(
            (review.declarations.map { it.fixture } + converted).toSet(),
            inventory.examples
                .filter { it.disposition == RepairDisposition.OVERRIDDEN }
                .map { it.fixture }
                .toSet(),
        )
        assertEquals(0, report.overrides.size)
        assertEquals(0, report.unresolved.size)
    }

    /**
     * The committed references still hold: each is exact, keeps its source digests and has moved,
     * and every never-locked example is referenced.
     */
    @Test
    fun committedReferencesMatchTheTree() {
        val text = read("audits/catalog/legacy-references.json")
        val report = AuditJson.legacyReferences(text)
        assertEquals(text, AuditJson.write(report))
        val inventory = AuditJson.curatedRepairs(read("audits/catalog/curated-repairs.json"))
        FormMigration.verifyReferences(report, moved())
        report.references.forEach { assertEquals(40, it.pageBlob.length) }
        assertEquals(
            inventory.examples
                .filter { it.disposition == RepairDisposition.UNCORRECTED && it.markers.isEmpty() }
                .map { it.fixture }
                .toSet(),
            (report.references.map { it.fixture } + report.unresolved.map { it.fixture }).toSet(),
        )
        assertEquals(250, report.references.size)
        assertEquals(0, report.unresolved.size)
    }

    /** A reference whose fixture never moved, or whose source text changed, is rejected. */
    @Test
    fun referenceChecksRejectDrift() {
        val report = AuditJson.legacyReferences(read("audits/catalog/legacy-references.json"))
        val first = report.references.first()
        val unmoved =
            assertThrows(IllegalArgumentException::class.java) {
                FormMigration.verifyReferences(report, moved() - first.fixture)
            }
        assertTrue("${first.fixture}: a referenced fixture has not moved" in unmoved.message!!)

        val block = first.blocks.first()
        val edited =
            report.copy(
                references =
                    listOf(first.copy(blocks = listOf(block.copy(text = block.text + " ")))) +
                        report.references.drop(1)
            )
        val digest =
            assertThrows(IllegalArgumentException::class.java) {
                FormMigration.verifyReferences(edited, moved())
            }
        assertTrue(
            "${first.fixture}: ${block.role} source text digest mismatch" in digest.message!!,
            digest.message,
        )
    }
}
