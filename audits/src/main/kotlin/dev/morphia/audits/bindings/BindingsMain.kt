package dev.morphia.audits.bindings

import dev.morphia.audits.coverage.AuditJson
import java.nio.file.Files
import java.nio.file.Path

/**
 * Imports the physical method/fixture bindings and writes the evidence catalog and the core
 * descriptor to explicit output paths. It reads source and fixture trees; it never edits them.
 */
fun main(args: Array<String>) {
    require(args.size == 5) {
        "Usage: BindingsMainKt <repository-root> <revision> <policy.json> <catalog-out> <descriptor-out>"
    }
    val policy =
        AuditJson.bindingPolicy(
            Files.readAllBytes(Path.of(args[2])).decodeToString(throwOnInvalidSequence = true)
        )
    val catalog = LegacyBindings.capture(Path.of(args[0]), args[1], policy)
    Files.writeString(Path.of(args[3]), AuditJson.write(catalog))
    Files.writeString(Path.of(args[4]), AuditJson.write(catalog.descriptor()))
}
