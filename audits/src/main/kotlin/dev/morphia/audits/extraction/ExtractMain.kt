package dev.morphia.audits.extraction

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.sources.SourceBundle
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * Inventories the pinned docs pages, writes a `scenario.json` for every example that has none, and
 * rewrites `documented-examples.json`. It exits with 3 when an example can't be extracted; the
 * failures are printed and also fail the audit's missing-scenario check until fixed or excluded.
 */
fun main(args: Array<String>) {
    try {
        require(args.size == 4) {
            "Usage: ExtractMainKt <repository> <bundle> <content-sha256> <documents.json>"
        }
        val repository = Path.of(args[0]).toRealPath()
        val bundle = SourceBundle.open(Path.of(args[1]), args[2])
        val document = AuditJson.documents(Files.readString(Path.of(args[3])))
        val plan =
            AuditJson.scenarioPlan(
                Files.readString(repository.resolve("audits/catalog/scenario-plan.json"))
            )
        val overrides = AuditJson.overrides(Files.readString(repository.resolve(Overrides.PATH)))
        val inventory = DocumentedExamples.capture(repository, bundle, document, plan)
        val result = Extraction.extract(repository, inventory, overrides)
        result.written.forEach { (path, text) ->
            val file = repository.resolve(path)
            Files.createDirectories(file.parent)
            Files.writeString(file, text)
        }
        Files.writeString(
            repository.resolve(DocumentedExamples.PATH),
            AuditJson.write(result.inventory) + "\n",
        )
        println("Wrote ${result.written.size} scenarios")
        result.failures.forEach {
            println("NOT EXTRACTED ${it.page} \"${it.title}\": ${it.detail}")
        }
        if (result.failures.isNotEmpty()) exitProcess(3)
    } catch (failure: IllegalArgumentException) {
        System.err.println("EXTRACT_INVALID: ${failure.message}")
        exitProcess(2)
    }
}
