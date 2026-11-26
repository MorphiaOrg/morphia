package dev.morphia.audits.documents

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.sources.SourceBundle
import java.nio.file.Path
import kotlin.system.exitProcess

/** Reports to stdout; never changes the bundle, accepted catalog or generated test corpus. */
fun main(args: Array<String>) {
    try {
        require(args.size >= 5) {
            "Usage: DocumentsMainKt <bundle> <content-sha256> <isolated-python> <adapter.py> <page-id>..."
        }
        val report =
            DocumentParsing.parse(
                SourceBundle.open(Path.of(args[0]), args[1]),
                args.drop(4),
                Path.of(args[2]),
                Path.of(args[3]),
                // Every operator page parses in one run of a few minutes.
                timeoutSeconds = 600,
                maximumOutputBytes = 128L * 1024 * 1024,
            )
        System.out.write(AuditJson.write(report).toByteArray(Charsets.UTF_8))
        System.out.flush()
        if (report.state == DocumentState.PARTIAL) exitProcess(3)
    } catch (failure: Exception) {
        System.err.println("PARSER_INVALID: ${failure.message ?: failure.javaClass.simpleName}")
        exitProcess(2)
    }
}
