package dev.morphia.audits.documents

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.sources.VerifiedSourceBundle
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Arrays
import java.util.Base64
import java.util.HexFormat
import java.util.concurrent.TimeUnit

/** Explicit, bounded native runtime boundary. Never invoked by ordinary Maven tests/builds. */
object DocumentParsing {
    const val PARSER_REVISION = "0c26866083a02177a52ade1ccb17cc61a2816be7"

    fun export(bundle: VerifiedSourceBundle, pages: List<String>): DocumentInput {
        require(pages.isNotEmpty() && pages.distinct().size == pages.size)
        require(pages.all { validPath(it) && it.endsWith(".txt") })
        return DocumentInput(
            sourceSha256 = bundle.manifest.contentSha256,
            projectRoot = bundle.manifest.request.projectRoot,
            pages =
                pages.sortedWith { left, right ->
                    Arrays.compareUnsigned(
                        left.toByteArray(Charsets.UTF_8),
                        right.toByteArray(Charsets.UTF_8),
                    )
                },
            resources =
                bundle.manifest.resources
                    .sortedBy { it.path }
                    .map { resource ->
                        // Links are transported as metadata; read() deliberately resolves links.
                        val bytes =
                            if (resource.linkTarget != null)
                                resource.linkTarget.toByteArray(Charsets.UTF_8)
                            else bundle.read(resource.path).bytes
                        DocumentResource(
                            resource.path,
                            resource.kind,
                            resource.sha256,
                            Base64.getEncoder().encodeToString(bytes),
                            resource.linkTarget,
                        )
                    },
            sourceProblems = bundle.manifest.problems,
        )
    }

    fun parse(
        bundle: VerifiedSourceBundle,
        pages: List<String>,
        python: Path,
        adapter: Path,
        timeoutSeconds: Long = 120,
        maximumOutputBytes: Long = 64L * 1024 * 1024,
    ): DocumentReport {
        require(timeoutSeconds in 1..600 && maximumOutputBytes in 1..128L * 1024 * 1024)
        require(python.isAbsolute && Files.isExecutable(python)) {
            "Explicit isolated Python executable required"
        }
        require(
            adapter.isAbsolute &&
                Files.isRegularFile(adapter) &&
                Files.isRegularFile(adapter.resolveSibling("uv.lock"))
        )
        val input = export(bundle, pages)
        val bytes = AuditJson.write(input).toByteArray(Charsets.UTF_8)
        require(bytes.size <= 64L * 1024 * 1024) { "Parser input exceeds 64 MiB" }
        val temporary = Files.createTempDirectory("morphia-document-command-")
        var process: Process? = null
        try {
            val inputFile = temporary.resolve("input.json")
            Files.write(inputFile, bytes)
            val stdout = temporary.resolve("stdout.json")
            val stderr = temporary.resolve("stderr.txt")
            val running =
                ProcessBuilder(
                        python.toString(),
                        "-I",
                        "-B",
                        adapter.toString(),
                        inputFile.toString(),
                    )
                    .directory(temporary.toFile())
                    .redirectOutput(stdout.toFile())
                    .redirectError(stderr.toFile())
                    .apply {
                        environment().clear()
                        environment()["LANG"] = "C.UTF-8"
                    }
                    .start()
            process = running
            running.outputStream.close()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
            while (!running.waitFor(50, TimeUnit.MILLISECONDS)) {
                require(System.nanoTime() < deadline) { "Parser deadline exceeded" }
                require(
                    Files.size(stdout) <= maximumOutputBytes && Files.size(stderr) <= 1024 * 1024
                ) {
                    "Parser output budget exceeded"
                }
            }
            require(Files.size(stdout) <= maximumOutputBytes && Files.size(stderr) <= 1024 * 1024) {
                "Parser output budget exceeded"
            }
            require(running.exitValue() in setOf(0, 3)) {
                "Parser failed: ${Files.newInputStream(stderr).use { it.readNBytes(4096).decodeToString() }}"
            }
            val report =
                AuditJson.documents(
                    Files.readAllBytes(stdout).decodeToString(throwOnInvalidSequence = true)
                )
            require(
                report.sourceSha256 == input.sourceSha256 && report.inputSha256 == digest(bytes)
            ) {
                "Parser input provenance mismatch"
            }
            require(
                report.parser.engine == "snooty" &&
                    report.parser.revision == PARSER_REVISION &&
                    report.parser.pythonVersion == "3.12.16" &&
                    report.parser.adapterSha256 == digest(Files.readAllBytes(adapter)) &&
                    report.parser.dependencyLockSha256 ==
                        digest(Files.readAllBytes(adapter.resolveSibling("uv.lock")))
            ) {
                "Parser runtime provenance mismatch"
            }
            require(
                report.pages.map { it.id } == input.pages &&
                    (running.exitValue() == 0) == (report.state == DocumentState.READY)
            ) {
                "Parser response scope/state mismatch"
            }
            validateOrigins(report, bundle)
            return report
        } finally {
            process?.let { running ->
                running.descendants().use { children -> children.forEach { it.destroyForcibly() } }
                if (running.isAlive) running.destroyForcibly()
                running.waitFor(5, TimeUnit.SECONDS)
            }
            Files.walk(temporary).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }

    internal fun validateOrigins(report: DocumentReport, bundle: VerifiedSourceBundle) {
        val resources = bundle.manifest.resources.associateBy { it.path }
        fun check(origin: DocumentOrigin) {
            require((origin.path == null) == (origin.sha256 == null)) {
                "Incomplete document origin"
            }
            origin.path?.let { path ->
                val resource = resources[path]
                require(
                    resource != null &&
                        resource.kind != dev.morphia.audits.sources.GitResourceKind.SYMLINK &&
                        resource.sha256 == origin.sha256
                ) {
                    "Document origin differs from verified source: $path"
                }
            }
            require(origin.reportedLine == null || origin.reportedLine > 0)
            require(origin.precision != DocumentPrecision.PARSER_REPORTED || origin.path != null)
            require(
                origin.precision != DocumentPrecision.GENERATED_OR_UNKNOWN ||
                    origin.reportedLine == null
            )
            require(origin.includeChain.all(::validPath)) { "Invalid include provenance path" }
        }
        require(report.diagnostics.all { validPath(it.path) && (it.line == null || it.line > 0) }) {
            "Invalid diagnostic source path"
        }
        report.pages.forEach { page ->
            val pending = ArrayDeque<DocumentNode>()
            page.raw?.let(pending::add)
            page.expanded?.let(pending::add)
            while (pending.isNotEmpty()) {
                val node = pending.removeFirst()
                require(node.kind.isNotBlank())
                check(node.origin)
                pending.addAll(node.children)
                pending.addAll(node.arguments)
            }
            page.dependencies.forEach {
                check(it.origin)
                if (page.dependenciesComplete)
                    require(
                        it.origin.path != null &&
                            it.parserDigest?.matches(Regex("(?:[0-9a-f]{64}|[0-9a-f]{128})")) ==
                                true
                    ) {
                        "Complete dependency lacks source/digest evidence"
                    }
            }
        }
    }

    private fun validPath(value: String) =
        value.isNotBlank() &&
            !value.startsWith('/') &&
            '\\' !in value &&
            value.split('/').none { it in setOf("", ".", "..") } &&
            value.none { it.code < 32 || it.code == 127 } &&
            value.codePoints().noneMatch { it in 0xD800..0xDFFF }

    private fun digest(bytes: ByteArray) =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
}
