package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.repairs.FixtureMigration
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FixtureMigrationTest {
    private val root =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("core/src/test/java/dev/morphia/test")) }

    private fun read(path: String) =
        Files.readAllBytes(root.resolve(path)).decodeToString(throwOnInvalidSequence = true)

    private val ledger = AuditJson.fixtureMigration(read("audits/catalog/fixture-migration.json"))

    @Test
    fun scenariosAreTheirOverridesDocumentedSource() {
        ledger.migrations.forEach {
            assertEquals(FixtureMigration.scenario(it.override, it.ordered), read(it.scenario))
        }
    }

    @Test
    fun removedFilesAreDeclaredWithTheirDigests() {
        val retired = FixtureMigration.retired(ledger)
        ledger.migrations.forEach { migrated ->
            assertTrue(
                retired.containsKey(
                    "core/src/test/resources/dev/morphia/test/${migrated.fixture}/${migrated.override.retiredLock.name}"
                )
            )
            migrated.override.fixtureFiles.forEach { (name, digest) ->
                assertEquals(
                    digest,
                    retired["core/src/test/resources/dev/morphia/test/${migrated.fixture}/$name"],
                )
            }
        }
    }

    @Test
    fun aMethodThatChangedBeforeItMovedIsRejected() {
        val bindings = AuditJson.legacyBindings(read("audits/catalog/legacy-bindings.json"))
        val ledgerNames = AuditJson.nameBackport(read("audits/catalog/name-backport.json"))
        val projected =
            dev.morphia.audits.bindings.NameBackport.project(
                bindings,
                ledgerNames,
                bindings.repositoryRevision,
            )
        val moved = ledger.migrations.map { it.legacyType to it.legacyMethod }.toSet()
        val removed = FixtureMigration.remove(projected, ledger)
        assertEquals(projected.methods.size - moved.size, removed.methods.size)
        val tampered =
            ledger.copy(
                migrations = ledger.migrations.map { it.copy(legacyBodySha256 = "0".repeat(64)) }
            )
        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                FixtureMigration.remove(projected, tampered)
            }
        assertTrue("changed before it moved" in failure.message!!, failure.message)
    }

    @Test
    fun theLedgersOverrideMustMatchItsReviewedDeclaration() {
        val review = AuditJson.formReview(read("audits/catalog/form-review.json"))
        val inventory = AuditJson.curatedRepairs(read("audits/catalog/curated-repairs.json"))
        FixtureMigration.verify(root, ledger, inventory, review)
        val repinned =
            ledger.copy(
                migrations =
                    ledger.migrations.map {
                        it.copy(override = it.override.copy(pageBlob = "0".repeat(40)))
                    }
            )
        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                FixtureMigration.verify(root, repinned, inventory, review)
            }
        assertTrue("differs from its review" in failure.message!!, failure.message)
        val unreviewed = review.copy(migrated = emptyList())
        assertThrows(IllegalArgumentException::class.java) {
            FixtureMigration.verify(root, ledger, inventory, unreviewed)
        }
    }

    @Test
    fun onlyExactOverridesCanBeMigrated() {
        val form = AuditJson.formMigration(read("audits/catalog/form-migration.json"))
        val withRules = form.overrides.first { it.rules.isNotEmpty() }
        assertThrows(IllegalArgumentException::class.java) {
            FixtureMigration.scenario(withRules, false)
        }
    }
}
