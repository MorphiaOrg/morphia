package dev.morphia.audits.discovery

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.documents.DocumentParsing
import dev.morphia.audits.sources.SourceBundle
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

/** Offline candidate output only. No accepted catalog, test or GitHub mutation. */
fun main(args: Array<String>) {
    try {
        require(args.size == 4 || (args.size == 6 && args[2] == "--parse")) {
            "Usage: DiscoveryMainKt <bundle> <source-sha256> (<document-report.json> <plan.json> | --parse <isolated-python> <adapter.py> <plan.json>)"
        }
        val bundle = SourceBundle.open(Path.of(args[0]), args[1])
        val plan =
            CatalogDiscovery.canonical(
                AuditJson.discoveryPlan(Files.readString(Path.of(args.last())))
            )
        val document =
            if (args.size == 6) {
                val prefix =
                    bundle.manifest.request.projectRoot
                        .takeUnless { it == "." }
                        ?.let { "$it/source/" } ?: "source/"
                val pages =
                    bundle.manifest.resources
                        .map { it.path }
                        .filter { it.startsWith(prefix) }
                        .map { it.removePrefix(prefix) }
                        .filter { page ->
                            page.endsWith(".txt") &&
                                (plan.definitionRoots.any { DocumentFacts.within(page, it) } ||
                                    plan.catalogs.any { DocumentFacts.catalogPage(page, it) })
                        }
                        .distinct()
                        .sorted()
                DocumentParsing.parse(
                    bundle,
                    pages,
                    Path.of(args[3]),
                    Path.of(args[4]),
                    timeoutSeconds = 600,
                    maximumOutputBytes = 128L * 1024 * 1024,
                )
            } else {
                val path = Path.of(args[2])
                require(Files.size(path) <= 512L * 1024 * 1024) {
                    "Document report exceeds the 512 MiB artifact budget"
                }
                AuditJson.documents(Files.readString(path))
            }
        val report = CatalogDiscovery.normalize(bundle, document, plan)
        System.out.write(AuditJson.write(report).toByteArray(Charsets.UTF_8))
        System.out.flush()
        if (report.state == DiscoveryState.PARTIAL) exitProcess(3)
    } catch (failure: Exception) {
        System.err.println("DISCOVERY_INVALID: ${failure.message ?: failure.javaClass.simpleName}")
        exitProcess(2)
    }
}
