package dev.morphia.audits.repairs

import dev.morphia.audits.coverage.AuditJson
import java.nio.file.Files
import java.nio.file.Path

/** Writes the curated-repair inventory to an explicit output path; sources are only read. */
fun main(args: Array<String>) {
    require(args.size == 5) {
        "Usage: RepairsMainKt <repository-root> <revision> <review.json> <form-review.json> <out>"
    }
    val root = Path.of(args[0])
    fun read(path: Path) = Files.readAllBytes(path).decodeToString(throwOnInvalidSequence = true)
    val report =
        CuratedRepairs.capture(
            root,
            args[1],
            AuditJson.bindingDescriptor(
                read(root.resolve("core/src/test/resources/dev/morphia/test/example-bindings.json"))
            ),
            AuditJson.repairReview(read(Path.of(args[2]))),
            FixtureMigration.retiredLocks(
                AuditJson.formReview(read(Path.of(args[3]))),
                AuditJson.fixtureMigration(
                    read(root.resolve("audits/catalog/fixture-migration.json"))
                ),
            ),
        )
    Files.writeString(Path.of(args[4]), AuditJson.write(report))
}
