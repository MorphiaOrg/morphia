package dev.morphia.audits.scenarios

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.sources.SourceBundle
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

/** Read-only candidate projection. Reports contain snapshots and are deliberately write-only. */
fun main(args: Array<String>) {
    try {
        require(args.size == 4) {
            "Usage: ScenariosMainKt <bundle> <source-sha256> <documents.json> <plan.json>"
        }
        val bundle = SourceBundle.open(Path.of(args[0]), args[1])
        fun read(path: String, maximum: Int): String {
            val bytes = Files.newInputStream(Path.of(path)).use { it.readNBytes(maximum + 1) }
            require(bytes.size <= maximum) { "Scenario input exceeds its artifact budget" }
            return bytes.decodeToString(throwOnInvalidSequence = true)
        }
        val document = AuditJson.documents(read(args[2], 512 * 1024 * 1024))
        val plan = AuditJson.scenarioPlan(read(args[3], 1024 * 1024))
        val report = ScenarioExtraction.extract(bundle, document, plan)
        System.out.write(AuditJson.write(report).toByteArray(Charsets.UTF_8))
        System.out.flush()
        require(!System.out.checkError()) { "Cannot write complete scenario artifact to stdout" }
        if (report.state == ScenarioExtractionState.PARTIAL) exitProcess(3)
    } catch (failure: IllegalArgumentException) {
        invalid(failure)
    } catch (failure: java.io.IOException) {
        invalid(failure)
    }
}

private fun invalid(failure: Exception): Nothing {
    System.err.println("SCENARIOS_INVALID: ${failure.message ?: failure.javaClass.simpleName}")
    exitProcess(2)
}
