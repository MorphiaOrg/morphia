package dev.morphia.audits.sources

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** Own the entire batch-process lifetime, including failures partway through setup. */
internal class GitObjectSession
private constructor(
    val errors: Path,
    private val process: Process,
    val input: BufferedOutputStream,
    val output: BufferedInputStream,
    private val deadline: ScheduledExecutorService,
) : AutoCloseable {
    companion object {
        fun open(repository: Path, timeoutSeconds: Long): GitObjectSession {
            require(timeoutSeconds in 1..600)
            val errors = Files.createTempFile("morphia-git-objects-", ".log")
            var process: Process? = null
            var deadline: ScheduledExecutorService? = null
            try {
                val batch =
                    GitCommands.builder(repository, listOf("cat-file", "--batch"))
                        .redirectError(errors.toFile())
                        .start()
                process = batch
                val input = BufferedOutputStream(batch.outputStream)
                val output = BufferedInputStream(batch.inputStream)
                val timer =
                    Executors.newSingleThreadScheduledExecutor { action ->
                        Thread(action, "audit-git-deadline").apply { isDaemon = true }
                    }
                deadline = timer
                timer.schedule({ GitCommands.terminate(batch) }, timeoutSeconds, TimeUnit.SECONDS)
                return GitObjectSession(errors, batch, input, output, timer)
            } catch (failure: Throwable) {
                // Preserve fatal failures; cleanup must not turn them into a successful report.
                runCatching { deadline?.shutdownNow() }
                runCatching {
                    process?.let {
                        GitCommands.terminate(it)
                        it.waitFor(5, TimeUnit.SECONDS)
                    }
                }
                runCatching { Files.deleteIfExists(errors) }
                throw failure
            }
        }
    }

    override fun close() {
        deadline.shutdownNow()
        try {
            runCatching { input.close() }
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                GitCommands.terminate(process)
                process.waitFor(5, TimeUnit.SECONDS)
            }
        } finally {
            runCatching { GitCommands.terminate(process) }
            runCatching { output.close() }
            Files.deleteIfExists(errors)
        }
    }
}
