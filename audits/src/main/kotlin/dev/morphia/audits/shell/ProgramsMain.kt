package dev.morphia.audits.shell

import dev.morphia.audits.coverage.AuditJson
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

/** Read-only transport. Regenerate plans from source requests; never hydrate encoded snapshots. */
fun main(args: Array<String>) {
    val request =
        try {
            require(args.size == 1) { "Usage: ProgramsMainKt <program-request.json>" }
            val bytes =
                Files.newInputStream(Path.of(args[0])).use { it.readNBytes(16 * 1024 * 1024 + 1) }
            require(bytes.size <= 16 * 1024 * 1024) { "Program input exceeds transport limit" }
            AuditJson.programRequest(bytes.decodeToString(throwOnInvalidSequence = true))
        } catch (failure: Exception) {
            emit(
                ShellProgramResult(
                    state = ShellLiteralState.INVALID,
                    parser = ShellPrograms.PARSER,
                    dialect = ShellDialect.UNSPECIFIED,
                    sources = emptyList(),
                    bindings = emptyList(),
                    commands = emptyList(),
                    expectations = emptyList(),
                    semanticSha256 = null,
                    problems =
                        listOf(
                            ProgramProblem(
                                "INVALID_INPUT",
                                failure.message ?: failure.javaClass.simpleName,
                                null,
                            )
                        ),
                )
            )
            return
        }
    // Unexpected normalization/implementation failures must remain visible to the caller.
    emit(ShellPrograms.normalize(request))
}

private fun emit(result: ShellProgramResult) {
    System.out.write(AuditJson.write(result).toByteArray(Charsets.UTF_8))
    System.out.flush()
    when (result.state) {
        ShellLiteralState.NORMALIZED -> Unit
        ShellLiteralState.UNSUPPORTED -> exitProcess(3)
        ShellLiteralState.INVALID -> exitProcess(2)
    }
}
