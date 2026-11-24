package dev.morphia.audits.repairs

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.generate.Candidates
import java.nio.file.Files
import java.nio.file.Path

private const val USAGE =
    """Usage: FormMainKt manifest <repository-root>
       FormMainKt rescenario <repository-root>"""

/**
 * Maintains the frozen MORPHIA_FORM migration ledger, `audits/catalog/fixture-migration.json`. The
 * commands that drafted, captured and converted it read historical pages with the retired RST
 * extractor and are gone; the ledger, the review and the references stay as reviewed records.
 * `rescenario` rewrites the ledger's scenarios from their recorded overrides, and `manifest` writes
 * the case manifest from it.
 */
fun main(args: Array<String>) {
    fun read(path: String) =
        Files.readAllBytes(Path.of(path)).decodeToString(throwOnInvalidSequence = true)
    when (args.firstOrNull()) {
        "rescenario" -> {
            require(args.size == 2) { USAGE }
            val path = Path.of("${args[1]}/audits/catalog/fixture-migration.json")
            val ledger = AuditJson.fixtureMigration(read(path.toString()))
            Files.writeString(
                path,
                AuditJson.write(FixtureMigration.rewrite(Path.of(args[1]), ledger)),
            )
        }
        "manifest" -> {
            require(args.size == 2) { USAGE }
            val root = Path.of(args[1])
            val ledger =
                AuditJson.fixtureMigration(read("${args[1]}/audits/catalog/fixture-migration.json"))
            Files.writeString(
                root.resolve(CaseManifests.PATH),
                AuditJson.write(Candidates.manifest(root, ledger)),
            )
        }
        else -> throw IllegalArgumentException(USAGE)
    }
}
