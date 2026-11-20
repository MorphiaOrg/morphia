package dev.morphia.audits.repairs

import dev.morphia.audits.coverage.AuditJson
import java.nio.file.Files
import java.nio.file.Path

private const val USAGE =
    """Usage: FormMainKt draft <repository-root> <pages> <source-repository> <review-out> <candidates.json>...
       FormMainKt capture <repository-root> <revision> <pages> <review.json> <out>
       FormMainKt apply <repository-root> <report.json>"""

/**
 * Drafts, captures and applies the MORPHIA_FORM lock migration. Pages are read from an explicit
 * directory laid out as `<pages>/<commit>/<path>`; nothing is fetched.
 */
fun main(args: Array<String>) {
    fun read(path: String) =
        Files.readAllBytes(Path.of(path)).decodeToString(throwOnInvalidSequence = true)
    when (args.firstOrNull()) {
        "draft" -> {
            require(args.size >= 6) { USAGE }
            val review =
                FormMigration.draft(
                    Path.of(args[1]),
                    Path.of(args[2]),
                    args[3],
                    args.drop(5).map { AuditJson.formCandidates(read(it)).candidates },
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
                )
            Files.writeString(Path.of(args[5]), AuditJson.write(report))
        }
        "apply" -> {
            require(args.size == 3) { USAGE }
            FormMigration.apply(Path.of(args[1]), AuditJson.formMigration(read(args[2])))
        }
        else -> throw IllegalArgumentException(USAGE)
    }
}
