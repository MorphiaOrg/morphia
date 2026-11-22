package dev.morphia.audits.coverage

import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

/** Explicit transport command. It reads three artifacts and writes one report to stdout. */
fun main(args: Array<String>) {
    val markdown = args.firstOrNull() == "--markdown"
    val inputs = if (markdown) args.drop(1) else args.toList()
    val report =
        try {
            require(inputs.size == 3) {
                "Usage: CoverageMainKt [--markdown] <catalog.json> <plan.json> <api-inventory.json>"
            }
            fun read(path: String): String =
                Files.readAllBytes(Path.of(path)).decodeToString(throwOnInvalidSequence = true)
            CoverageComparison.compare(
                AuditJson.catalog(read(inputs[0])),
                AuditJson.plan(read(inputs[1])),
                AuditJson.api(read(inputs[2])),
            )
        } catch (error: Exception) {
            CoverageReport(
                state = ReportState.INVALID,
                catalogSha256 = null,
                planSha256 = null,
                apiSha256 = null,
                contexts = emptyList(),
                capabilities = emptyList(),
                problems =
                    listOf(
                        AuditProblem(
                            "INVALID_INPUT",
                            error.message ?: error.javaClass.simpleName,
                            null,
                            null,
                        )
                    ),
            )
        }
    val output = if (markdown) CoverageMarkdown.render(report) else AuditJson.write(report)
    System.out.write(output.toByteArray(Charsets.UTF_8))
    System.out.flush()
    when (report.state) {
        ReportState.INVALID -> exitProcess(2)
        ReportState.PARTIAL -> exitProcess(3)
        ReportState.COMPARED -> Unit
    }
}

object CoverageMarkdown {
    fun render(report: CoverageReport): String = buildString {
        appendLine("Operator audit: ${report.state}")
        appendLine()
        appendLine("A completed comparison does not assert serialization or behavioral coverage.")
        appendLine()
        appendLine("| Context | Observed capabilities | Minimum expected | Discovery |")
        appendLine("| --- | ---: | ---: | --- |")
        report.contexts.forEach {
            appendLine(
                "| ${it.context.key()} | ${it.observed} | ${it.expectedMinimum} | ${if (it.healthy) "healthy" else "unresolved"} |"
            )
        }
        appendLine()
        appendLine(
            "| Capability | Documentation | API | Introduction | Serialization | Behavior | Examples |"
        )
        appendLine("| --- | --- | --- | --- | --- | --- | --- |")
        report.capabilities.forEach { row ->
            appendLine(
                "| ${row.capability.id()} | ${row.documentation} | ${row.api} | ${row.introduction.version?.display() ?: row.introduction.state} | ${row.serialization} | ${row.behavior} | ${row.examples} |"
            )
        }
        report.capabilities.forEach { row ->
            val details =
                row.notes +
                    row.exampleProblems.map { "Example extraction: $it" } +
                    row.annotations
                        .filter { it.state != AnnotationState.MINIMUM_MATCH }
                        .map { "${it.declaration}: server annotation ${it.state}" }
            if (details.isNotEmpty()) {
                appendLine()
                details.forEach { appendLine("- ${row.capability.id()}: ${it.replace('\n', ' ')}") }
            }
        }
        if (report.problems.isNotEmpty()) {
            appendLine()
            report.problems.forEach {
                val location = listOfNotNull(it.context?.key(), it.subject).joinToString(" / ")
                appendLine(
                    "- ${it.code}${if (location.isEmpty()) "" else " ($location)"}: ${it.message.replace('\n', ' ')}"
                )
            }
        }
    }
}
