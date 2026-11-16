package dev.morphia.audits.sources

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * One native tool boundary. Implicit fetch and replacement objects are disabled even for explicit
 * fetch jobs.
 */
internal object GitCommands {
    fun builder(repository: Path, arguments: List<String>): ProcessBuilder =
        ProcessBuilder(
                listOf(
                    "git",
                    "--no-replace-objects",
                    "--no-lazy-fetch",
                    "--literal-pathspecs",
                    "-C",
                    repository.toString(),
                ) + arguments
            )
            .apply {
                val env = environment()
                env.keys.filter { it.startsWith("GIT_") }.toList().forEach(env::remove)
                env["GIT_NO_REPLACE_OBJECTS"] = "1"
                env["GIT_TERMINAL_PROMPT"] = "0"
                env["GIT_CONFIG_NOSYSTEM"] = "1"
                env["GIT_CONFIG_GLOBAL"] =
                    if (System.getProperty("os.name").startsWith("Windows")) "NUL" else "/dev/null"
            }

    fun run(
        repository: Path,
        arguments: List<String>,
        timeoutSeconds: Long,
        input: ByteArray = byteArrayOf(),
        maximumOutput: Long = 64L * 1024 * 1024,
    ): ByteArray {
        require(
            timeoutSeconds in 1..600 &&
                maximumOutput in 1..128L * 1024 * 1024 &&
                input.size <= 8 * 1024 * 1024
        )
        val stdout = Files.createTempFile("morphia-git-", ".out")
        val stderr = Files.createTempFile("morphia-git-", ".err")
        var process: Process? = null
        val timer =
            Executors.newSingleThreadScheduledExecutor { action ->
                Thread(action, "audit-git-command-deadline").apply { isDaemon = true }
            }
        val stopped = AtomicReference<String?>()
        try {
            val running =
                builder(repository, arguments)
                    .redirectOutput(stdout.toFile())
                    .redirectError(stderr.toFile())
                    .start()
            process = running
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
            timer.scheduleAtFixedRate(
                {
                    try {
                        if (running.isAlive) {
                            val reason =
                                when {
                                    System.nanoTime() >= deadline ->
                                        "Git command exceeded its explicit deadline"
                                    Files.size(stdout) > maximumOutput ->
                                        "Git output budget exceeded"
                                    Files.size(stderr) > 256 * 1024 ->
                                        "Git diagnostic output budget exceeded"
                                    else -> null
                                }
                            if (reason != null && stopped.compareAndSet(null, reason))
                                terminate(running)
                        }
                    } catch (failure: Exception) {
                        if (stopped.compareAndSet(null, "Git command budget enforcement failed"))
                            terminate(running)
                    }
                },
                50,
                50,
                TimeUnit.MILLISECONDS,
            )
            try {
                running.outputStream.use { it.write(input) }
            } catch (failure: Exception) {
                stopped.get()?.let { error(it) }
                throw failure
            }
            if (!running.waitFor(timeoutSeconds + 1, TimeUnit.SECONDS)) {
                terminate(running)
                error("Git command exceeded its explicit deadline")
            }
            stopped.get()?.let { error(it) }
            require(Files.size(stdout) <= maximumOutput && Files.size(stderr) <= 256 * 1024) {
                "Git command output budget exceeded"
            }
            require(running.exitValue() == 0) {
                "Git command failed: ${Files.newInputStream(stderr).use { it.readNBytes(4096).decodeToString() }}"
            }
            return Files.readAllBytes(stdout)
        } finally {
            timer.shutdownNow()
            process?.let {
                if (it.isAlive) terminate(it)
                it.waitFor(5, TimeUnit.SECONDS)
            }
            Files.deleteIfExists(stdout)
            Files.deleteIfExists(stderr)
        }
    }

    fun terminate(process: Process) {
        process.descendants().use { descendants -> descendants.forEach { it.destroyForcibly() } }
        process.destroyForcibly()
    }
}
