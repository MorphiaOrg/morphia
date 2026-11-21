package dev.morphia.audits.repairs

import dev.morphia.audits.bindings.LegacyBindings
import dev.morphia.audits.coverage.AuditJson
import java.nio.file.Files
import java.nio.file.Path

private const val USAGE =
    """Usage: FormMainKt draft <repository-root> <pages> <source-repository> <review-out> <candidates.json>...
       FormMainKt capture <repository-root> <revision> <pages> <review.json> <out>
       FormMainKt apply <repository-root> <report.json>
       FormMainKt migrate <repository-root> <revision> <fixture> <test-type#method>
       FormMainKt convert <repository-root> <revision> <fixture>
       FormMainKt manifest <repository-root>"""

/**
 * Drafts, captures and applies the MORPHIA_FORM lock migration. Pages are read from an explicit
 * directory laid out as `<pages>/<commit>/<path>`; nothing is fetched. Lock categories and the
 * tests' extra collections come from the committed repair inventory,
 * `audits/catalog/curated-repairs.json`.
 */
fun main(args: Array<String>) {
    fun read(path: String) =
        Files.readAllBytes(Path.of(path)).decodeToString(throwOnInvalidSequence = true)
    fun facts(root: String) =
        FormMigration.facts(
            AuditJson.curatedRepairs(read("$root/audits/catalog/curated-repairs.json"))
        )
    when (args.firstOrNull()) {
        "draft" -> {
            require(args.size >= 6) { USAGE }
            val review =
                FormMigration.draft(
                    Path.of(args[1]),
                    Path.of(args[2]),
                    args[3],
                    args.drop(5).map { AuditJson.formCandidates(read(it)).candidates },
                    facts(args[1]),
                )
            Files.writeString(Path.of(args[4]), AuditJson.write(review))
        }
        "capture" -> {
            require(args.size == 6) { USAGE }
            val report =
                FormMigration.capture(
                    Path.of(args[1]),
                    args[2],
                    Path.of(args[3]),
                    AuditJson.formReview(read(args[4])),
                    facts(args[1]),
                )
            Files.writeString(Path.of(args[5]), AuditJson.write(report))
        }
        "apply" -> {
            require(args.size == 3) { USAGE }
            FormMigration.apply(Path.of(args[1]), AuditJson.formMigration(read(args[2])))
        }
        "migrate" -> {
            require(args.size == 5) { USAGE }
            val root = args[1]
            val ledgerPath = Path.of("$root/audits/catalog/fixture-migration.json")
            val ledger =
                if (Files.exists(ledgerPath))
                    AuditJson.fixtureMigration(read(ledgerPath.toString()))
                else FixtureMigrationLedger()
            val migrated =
                FixtureMigration.migrate(
                    Path.of(root),
                    AuditJson.formMigration(read("$root/audits/catalog/form-migration.json")),
                    AuditJson.curatedRepairs(read("$root/audits/catalog/curated-repairs.json")),
                    LegacyBindings.capture(
                        Path.of(root),
                        args[2],
                        AuditJson.bindingPolicy(read("$root/audits/catalog/binding-policy.json")),
                    ),
                    args[3],
                    args[4],
                )
            val migrations = (ledger.migrations + migrated).sortedBy { it.fixture }
            Files.writeString(ledgerPath, AuditJson.write(ledger.copy(migrations = migrations)))
        }
        "convert" -> {
            require(args.size == 4) { USAGE }
            val root = args[1]
            val ledgerPath = Path.of("$root/audits/catalog/fixture-migration.json")
            val ledger =
                if (Files.exists(ledgerPath))
                    AuditJson.fixtureMigration(read(ledgerPath.toString()))
                else FixtureMigrationLedger()
            val migrated =
                FixtureMigration.convert(
                    Path.of(root),
                    AuditJson.formMigration(read("$root/audits/catalog/form-migration.json")),
                    AuditJson.curatedRepairs(read("$root/audits/catalog/curated-repairs.json")),
                    LegacyBindings.capture(
                        Path.of(root),
                        args[2],
                        AuditJson.bindingPolicy(read("$root/audits/catalog/binding-policy.json")),
                    ),
                    args[3],
                )
            val migrations = (ledger.migrations + migrated).sortedBy { it.fixture }
            Files.writeString(ledgerPath, AuditJson.write(ledger.copy(migrations = migrations)))
        }
        "manifest" -> {
            require(args.size == 2) { USAGE }
            val root = Path.of(args[1])
            val ledger =
                AuditJson.fixtureMigration(read("${args[1]}/audits/catalog/fixture-migration.json"))
            Files.writeString(
                root.resolve(CaseManifests.PATH),
                AuditJson.write(CaseManifests.capture(root, ledger)),
            )
        }
        else -> throw IllegalArgumentException(USAGE)
    }
}
