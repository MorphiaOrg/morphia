package dev.morphia.audits.generate

import dev.morphia.audits.coverage.AuditJson
import java.nio.file.Files
import java.nio.file.Path

private const val USAGE =
    """Usage: GenerateMainKt candidate <repository-root> <out>
       GenerateMainKt integrate <repository-root> <out>
       GenerateMainKt promote <repository-root> <out>"""

/**
 * Generates a candidate of the operator-case namespace into `<out>`, integrates it in a temporary
 * worktree (writing `<out>/attestation.json` and `<out>/integration.log`), and promotes an attested
 * candidate into a checkout that still matches its base.
 */
fun main(args: Array<String>) {
    require(args.size == 3) { USAGE }
    val root = Path.of(args[1])
    val out = Path.of(args[2])
    when (args[0]) {
        "candidate" -> Candidates.candidate(root, out, Candidates.revision(root))
        "integrate" -> {
            val attestation =
                Candidates.integrate(root, out, LoggingRunner(out.resolve("integration.log")))
            Files.writeString(out.resolve("attestation.json"), AuditJson.write(attestation))
            require(attestation.passed()) { "Integration failed: ${attestation.results}" }
        }
        "promote" ->
            Candidates.promote(
                root,
                out,
                AuditJson.attestation(Files.readString(out.resolve("attestation.json"))),
                Candidates.revision(root),
            )
        else -> throw IllegalArgumentException(USAGE)
    }
}
