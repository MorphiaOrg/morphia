package dev.morphia.audits.sources

import java.nio.file.Files
import java.nio.file.Path

/**
 * Explicit acquisition only. It creates a new bare cache, pins a commit and hydrates selected blobs
 * in bulk.
 */
object GitAcquisition {
    fun fetch(request: GitSourceRequest, destination: Path) {
        GitSnapshot.validate(request)
        require(Files.isDirectory(destination.toAbsolutePath().parent)) {
            "Git cache parent must exist"
        }
        Files.createDirectory(destination)
        try {
            fun command(vararg arguments: String, input: ByteArray = byteArrayOf()): ByteArray =
                GitCommands.run(destination, arguments.toList(), request.timeoutSeconds, input)
            command("init", "--bare", "--quiet")
            command("config", "remote.origin.url", request.uri)
            command("config", "remote.origin.promisor", "true")
            command("config", "remote.origin.partialclonefilter", "blob:none")
            // All arguments are separate process values; neither URI nor pin is shell text.
            command(
                "-c",
                "gc.auto=0",
                "-c",
                "core.hooksPath=/dev/null",
                "-c",
                "protocol.allow=never",
                "-c",
                "protocol.https.allow=always",
                "-c",
                "protocol.file.allow=always",
                "fetch",
                "--quiet",
                "--filter=blob:none",
                "--depth=1",
                "--no-tags",
                "--no-write-fetch-head",
                "--no-auto-maintenance",
                "--recurse-submodules=no",
                "origin",
                request.revision,
            )
            val entries =
                GitObjects(destination, request.timeoutSeconds).use { git ->
                    git.commit(request.revision)
                    (request.roots + request.allowedSymlinkRoots)
                        .distinct()
                        .sorted()
                        .flatMap { root ->
                            git.entries(request.revision, root).also {
                                if (root in request.roots)
                                    require(it.isNotEmpty()) {
                                        "Selected source root is absent: $root"
                                    }
                            }
                        }
                        .distinctBy { it.path }
                }
            require(entries.size <= request.maximumFiles) {
                "Potential dependency closure exceeds the explicit file budget"
            }
            val objects = entries.map { it.objectId }.distinct().sorted()
            val input = (objects.joinToString("\n") + "\n").toByteArray(Charsets.US_ASCII)
            val before =
                command("cat-file", "--batch-check", input = input)
                    .decodeToString(throwOnInvalidSequence = true)
                    .lineSequence()
                    .filter(String::isNotBlank)
                    .toList()
            require(before.size == objects.size) { "Incomplete Git object availability check" }
            val missing =
                before.mapIndexedNotNull { index, value ->
                    if (value == objects[index] + " missing") objects[index]
                    else {
                        require(value.startsWith(objects[index] + " blob ")) {
                            "Unexpected Git object kind"
                        }
                        null
                    }
                }
            if (missing.isNotEmpty())
                command(
                    "-c",
                    "gc.auto=0",
                    "-c",
                    "fetch.negotiationAlgorithm=noop",
                    "-c",
                    "core.hooksPath=/dev/null",
                    "-c",
                    "protocol.allow=never",
                    "-c",
                    "protocol.https.allow=always",
                    "-c",
                    "protocol.file.allow=always",
                    "fetch",
                    "--quiet",
                    "--no-tags",
                    "--no-write-fetch-head",
                    "--no-auto-maintenance",
                    "--recurse-submodules=no",
                    "--filter=blob:none",
                    "--stdin",
                    "origin",
                    input = (missing.joinToString("\n") + "\n").toByteArray(Charsets.US_ASCII),
                )
            val hydrated =
                command("cat-file", "--batch-check", input = input)
                    .decodeToString(throwOnInvalidSequence = true)
                    .lineSequence()
                    .filter(String::isNotBlank)
                    .toList()
            require(hydrated.size == objects.size) { "Incomplete hydrated object check" }
            var total = 0L
            hydrated.forEachIndexed { index, line ->
                val fields = line.split(' ')
                require(fields.size == 3 && fields[0] == objects[index] && fields[1] == "blob") {
                    "Selected Git object was not hydrated"
                }
                val size = fields[2].toLongOrNull() ?: error("Invalid hydrated object size")
                require(
                    size in 0..request.maximumFileBytes && size <= request.maximumTotalBytes - total
                ) {
                    "Hydrated source exceeds explicit byte budgets"
                }
                total += size
            }
        } catch (failure: Exception) {
            runCatching {
                Files.walk(destination).use { paths ->
                    paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
                }
            }
            throw failure
        }
    }
}
