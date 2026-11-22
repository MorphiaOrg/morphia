package dev.morphia.audits.offline

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.repairs.CaseManifests
import dev.morphia.audits.repairs.FixtureMigration
import dev.morphia.audits.repairs.FormRule
import dev.morphia.audits.repairs.MIGRATED
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
            assertEquals(
                FixtureMigration.scenario(it.override, it.ordered, it.options),
                read(it.scenario),
            )
        }
    }

    @Test
    fun removedFilesAreDeclaredWithTheirDigests() {
        val retired = FixtureMigration.retired(ledger)
        ledger.migrations.forEach { migrated ->
            migrated.override.retiredLock?.let {
                assertTrue(
                    retired.containsKey(
                        "core/src/test/resources/dev/morphia/test/${migrated.fixture}/${it.name}"
                    )
                )
            }
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
        // Converted cases keep their legacy method; only migrated ones leave the capture.
        val moved =
            ledger.migrations
                .filter { it.kind == MIGRATED }
                .map { it.legacyType to it.legacyMethod }
                .toSet()
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
        val references = AuditJson.legacyReferences(read("audits/catalog/legacy-references.json"))
        FixtureMigration.verify(root, ledger, inventory, review, references)
        val repinned =
            ledger.copy(
                migrations =
                    ledger.migrations.map {
                        it.copy(override = it.override.copy(pageBlob = "0".repeat(40)))
                    }
            )
        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                FixtureMigration.verify(root, repinned, inventory, review, references)
            }
        assertTrue("differs from its review" in failure.message!!, failure.message)
        val unreviewed = review.copy(migrated = emptyList())
        assertThrows(IllegalArgumentException::class.java) {
            FixtureMigration.verify(root, ledger, inventory, unreviewed, references)
        }
    }

    @Test
    fun legacyOnlyCasesKeepTheirLegacyFixtures() {
        val text = read("audits/catalog/cutover.json")
        val cutover = AuditJson.cutover(text)
        assertEquals(text, AuditJson.write(cutover))
        FixtureMigration.verifyCutover(
            root,
            cutover,
            ledger,
            AuditJson.formMigration(read("audits/catalog/form-migration.json")),
            AuditJson.legacyReferences(read("audits/catalog/legacy-references.json")),
            AuditJson.curatedRepairs(read("audits/catalog/curated-repairs.json")),
        )
        val moved =
            cutover.copy(
                legacyOnly =
                    cutover.legacyOnly +
                        cutover.legacyOnly.first().copy(fixture = ledger.migrations.first().fixture)
            )
        assertThrows(IllegalArgumentException::class.java) {
            FixtureMigration.verifyCutover(
                root,
                moved,
                ledger,
                AuditJson.formMigration(read("audits/catalog/form-migration.json")),
                AuditJson.legacyReferences(read("audits/catalog/legacy-references.json")),
                AuditJson.curatedRepairs(read("audits/catalog/curated-repairs.json")),
            )
        }
        // A convertible case the batch skipped is caught: drop one legacy-only record.
        val skipped = cutover.copy(legacyOnly = cutover.legacyOnly.drop(1))
        assertThrows(IllegalArgumentException::class.java) {
            FixtureMigration.verifyCutover(
                root,
                skipped,
                ledger,
                AuditJson.formMigration(read("audits/catalog/form-migration.json")),
                AuditJson.legacyReferences(read("audits/catalog/legacy-references.json")),
                AuditJson.curatedRepairs(read("audits/catalog/curated-repairs.json")),
            )
        }
    }

    @Test
    fun theCommittedManifestListsEveryCaseWithOneOwner() {
        val manifest = AuditJson.caseManifest(read(CaseManifests.PATH))
        assertEquals(CaseManifests.capture(root, ledger), manifest)
        val descriptor =
            AuditJson.bindingDescriptor(
                read("core/src/test/resources/dev/morphia/test/example-bindings.json")
            )
        CaseManifests.owners(descriptor, manifest)
        // An example the manifest does not already own; a converted case names its legacy test.
        val owned = manifest.cases.map { it.name }.toSet()
        val unowned =
            descriptor.classes
                .flatMap { type ->
                    type.bindings.map {
                        "${type.resources.removePrefix("dev/morphia/test/")}/${it.resource}"
                    }
                }
                .first { it !in owned }
        val claimed =
            manifest.copy(cases = manifest.cases + manifest.cases.first().copy(name = unowned))
        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                CaseManifests.owners(descriptor, claimed)
            }
        assertTrue("more than one owner" in failure.message!!, failure.message)
    }

    @Test
    fun overridesWithRetypesOrCorrectionsCannotBeMigratedYet() {
        val form = AuditJson.formMigration(read("audits/catalog/form-migration.json"))
        listOf(
                form.overrides.first { it.retypes.isNotEmpty() },
                form.overrides.first { it.corrections.isNotEmpty() },
            )
            .forEach {
                assertThrows(IllegalArgumentException::class.java) {
                    FixtureMigration.scenario(it, true)
                }
            }
        // Declared rules travel with the scenario.
        val withRules =
            form.overrides.first {
                it.rules == listOf(FormRule.COND_ARRAY) &&
                    it.retypes.isEmpty() &&
                    it.corrections.isEmpty() &&
                    it.collections.isEmpty()
            }
        assertTrue("\"COND_ARRAY\"" in FixtureMigration.scenario(withRules, true))
    }
}
