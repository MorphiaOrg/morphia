package dev.morphia.audits.offline

import dev.morphia.audits.coverage.*
import dev.morphia.audits.documents.*
import dev.morphia.audits.sources.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Base64
import java.util.HexFormat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DocumentParsingTest {
    private fun digest(bytes: ByteArray) =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))

    private fun fixture(root: Path): VerifiedSourceBundle {
        Files.createDirectories(root.resolve("blobs"))
        val resources =
            listOf(
                    "manual/snooty.toml" to "name = 'fixture'\n",
                    "manual/source/index.txt" to "Title\n=====\n",
                    "examples/query.js" to "db.a.find({ s: 'a  b' });\n",
                )
                .map { (path, text) ->
                    val bytes = text.toByteArray(Charsets.UTF_8)
                    val hash = digest(bytes)
                    Files.write(root.resolve("blobs/$hash"), bytes)
                    GitResource(
                        path,
                        GitResourceKind.FILE,
                        "0".repeat(40),
                        hash,
                        bytes.size.toLong(),
                        null,
                    )
                }
        val request =
            GitSourceRequest(
                id = "fixture",
                uri = "https://example.invalid/docs.git",
                revision = "0".repeat(40),
                projectRoot = "manual",
                label = "8.0",
                role = SourceRole.CURRENT,
                channel = SourceChannel.RELEASED,
                roots = listOf("manual"),
                allowedSymlinkRoots = listOf("examples"),
                maximumFiles = 100,
                maximumFileBytes = 1024,
                maximumTotalBytes = 10000,
                timeoutSeconds = 10,
            )
        return VerifiedSourceBundle(
            root,
            GitSourceManifest(
                request = request,
                tree = "0".repeat(40),
                gitVersion = "fixture",
                contentSha256 = "1".repeat(64),
                state = SourceState.READY,
                resources = resources,
                problems = emptyList(),
            ),
        )
    }

    private fun response(input: DocumentInput, adapter: Path) =
        DocumentReport(
            sourceSha256 = input.sourceSha256,
            inputSha256 = digest(AuditJson.write(input).toByteArray(Charsets.UTF_8)),
            parser =
                DocumentParser(
                    "snooty",
                    DocumentParsing.PARSER_REVISION,
                    "0.20.26.dev0",
                    "3.12.16",
                    digest(Files.readAllBytes(adapter)),
                    digest(Files.readAllBytes(adapter.resolveSibling("uv.lock"))),
                    mapOf("snooty" to "0.20.26.dev0"),
                ),
            state = DocumentState.PARTIAL,
            pages = input.pages.map { DocumentPage(it, null, null, emptyList(), false) },
            diagnostics =
                listOf(
                    DocumentDiagnostic(
                        "MISSING_PARSED_PAGE",
                        input.pages.first(),
                        null,
                        "ERROR",
                        "Missing page",
                    )
                ),
            unresolvedUrls = emptyList(),
        )

    private fun adapter(root: Path): Path {
        val adapter = root.resolve("adapter.py")
        Files.writeString(adapter, "fixture runtime\n")
        Files.writeString(root.resolve("uv.lock"), "fixture lock\n")
        return adapter
    }

    private fun executable(root: Path, body: String): Path {
        val script = root.resolve("fake-python")
        Files.writeString(script, "#!/bin/sh\n$body\n")
        assertTrue(script.toFile().setExecutable(true))
        return script
    }

    @Test
    fun exportsExactBytesFromVerifiedSnapshotAndRejectsDrift(@TempDir root: Path) {
        val bundle = fixture(root)
        val exported = DocumentParsing.export(bundle, listOf("index.txt"))
        assertEquals(bundle.manifest.contentSha256, exported.sourceSha256)
        exported.resources.forEach {
            assertArrayEquals(
                bundle.read(it.path).bytes,
                Base64.getDecoder().decode(it.contentsBase64),
            )
        }
        Files.writeString(root.resolve("blobs/${exported.resources.first().sha256}"), "changed")
        assertThrows(UnresolvedSource::class.java) {
            DocumentParsing.export(bundle, listOf("index.txt"))
        }
    }

    @Test
    fun rejectsAmbiguousOrEscapingPageScopes(@TempDir root: Path) {
        val bundle = fixture(root)
        listOf(
                emptyList(),
                listOf("index.txt", "index.txt"),
                listOf("../outside.txt"),
                listOf("/absolute.txt"),
                listOf("x\\y.txt"),
                listOf("index.rst"),
            )
            .forEach { pages ->
                assertThrows(IllegalArgumentException::class.java) {
                    DocumentParsing.export(bundle, pages)
                }
            }
    }

    @Test
    fun checksResponseProvenanceAndExitStatus(@TempDir root: Path) {
        val bundle = fixture(root)
        val adapter = adapter(root)
        val report = response(DocumentParsing.export(bundle, listOf("index.txt")), adapter)
        val payload = root.resolve("response.json")
        Files.writeString(payload, AuditJson.write(report))
        val python = executable(root, "cat '${payload}'; exit 3")
        assertEquals(report, DocumentParsing.parse(bundle, listOf("index.txt"), python, adapter))
        Files.writeString(payload, AuditJson.write(report.copy(sourceSha256 = "2".repeat(64))))
        assertThrows(IllegalArgumentException::class.java) {
            DocumentParsing.parse(bundle, listOf("index.txt"), python, adapter)
        }
        Files.writeString(payload, AuditJson.write(report))
        executable(root, "cat '${payload}'; exit 0")
        assertThrows(IllegalArgumentException::class.java) {
            DocumentParsing.parse(bundle, listOf("index.txt"), python, adapter)
        }
    }

    @Test
    fun crossChecksReturnedOriginsAndScope(@TempDir root: Path) {
        val bundle = fixture(root)
        val adapter = adapter(root)
        val base = response(DocumentParsing.export(bundle, listOf("index.txt")), adapter)
        val resource = bundle.manifest.resources.first { it.path == "manual/source/index.txt" }
        val origin =
            DocumentOrigin(
                resource.path,
                resource.sha256,
                1,
                DocumentPrecision.PARSER_REPORTED,
                listOf(resource.path),
            )
        val node =
            DocumentNode(
                "root",
                origin,
                null,
                null,
                null,
                emptyList(),
                emptyMap(),
                emptyMap(),
                emptyList(),
                emptyList(),
            )
        val report =
            base.copy(pages = listOf(DocumentPage("index.txt", node, node, emptyList(), true)))
        val payload = root.resolve("response.json")
        val python = executable(root, "cat '${payload}'; exit 3")
        Files.writeString(payload, AuditJson.write(report))
        assertEquals(report, DocumentParsing.parse(bundle, listOf("index.txt"), python, adapter))
        Files.writeString(
            payload,
            AuditJson.write(
                report.copy(
                    pages =
                        listOf(
                            report.pages
                                .first()
                                .copy(
                                    raw = node.copy(origin = origin.copy(sha256 = "f".repeat(64)))
                                )
                        )
                )
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            DocumentParsing.parse(bundle, listOf("index.txt"), python, adapter)
        }
        Files.writeString(
            payload,
            AuditJson.write(base.copy(pages = listOf(base.pages.first().copy(id = "wrong.txt")))),
        )
        assertThrows(IllegalArgumentException::class.java) {
            DocumentParsing.parse(bundle, listOf("index.txt"), python, adapter)
        }
        Files.writeString(
            payload,
            AuditJson.write(
                report.copy(
                    pages =
                        listOf(
                            report.pages
                                .first()
                                .copy(
                                    raw =
                                        node.copy(origin = origin.copy(path = null, sha256 = null))
                                )
                        )
                )
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            DocumentParsing.parse(bundle, listOf("index.txt"), python, adapter)
        }
        val unknown =
            DocumentOrigin(null, null, null, DocumentPrecision.GENERATED_OR_UNKNOWN, emptyList())
        Files.writeString(
            payload,
            AuditJson.write(
                report.copy(
                    pages =
                        listOf(
                            report.pages
                                .first()
                                .copy(
                                    dependencies =
                                        listOf(
                                            DocumentDependency(
                                                "unknown.js",
                                                unknown,
                                                "0".repeat(128),
                                            )
                                        )
                                )
                        )
                )
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            DocumentParsing.parse(bundle, listOf("index.txt"), python, adapter)
        }
        Files.writeString(
            payload,
            AuditJson.write(
                report.copy(
                    diagnostics = listOf(report.diagnostics.first().copy(path = "../outside"))
                )
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            DocumentParsing.parse(bundle, listOf("index.txt"), python, adapter)
        }
        executable(root, "printf 'invalid runtime' >&2; exit 2")
        assertTrue(
            assertThrows(IllegalArgumentException::class.java) {
                    DocumentParsing.parse(bundle, listOf("index.txt"), python, adapter)
                }
                .message!!
                .contains("invalid runtime")
        )
    }

    @Test
    fun exportsLinkMetadataAndUsesCodePointScopeOrder(@TempDir root: Path) {
        val bundle = fixture(root)
        val bytes = "../../examples".toByteArray(Charsets.UTF_8)
        val link =
            GitResource(
                "manual/source/tested",
                GitResourceKind.SYMLINK,
                "0".repeat(40),
                digest(bytes),
                bytes.size.toLong(),
                "../../examples",
            )
        Files.write(root.resolve("blobs/${link.sha256}"), bytes)
        val linked =
            VerifiedSourceBundle(
                root,
                bundle.manifest.copy(resources = bundle.manifest.resources + link),
            )
        val exported = DocumentParsing.export(linked, listOf("\uD83D\uDE00.txt", "\uE000.txt"))
        assertEquals(listOf("\uE000.txt", "\uD83D\uDE00.txt"), exported.pages)
        assertArrayEquals(
            bytes,
            Base64.getDecoder()
                .decode(exported.resources.first { it.path == link.path }.contentsBase64),
        )
        assertEquals(
            "examples/query.js",
            linked.read("manual/source/tested/query.js").canonicalPath,
        )
    }

    @Test
    fun boundsRuntimeAndOutput(@TempDir root: Path) {
        val bundle = fixture(root)
        val adapter = adapter(root)
        val python = executable(root, "sleep 5")
        assertThrows(IllegalArgumentException::class.java) {
            DocumentParsing.parse(bundle, listOf("index.txt"), python, adapter, timeoutSeconds = 1)
        }
        executable(root, "printf 'too much output'")
        assertThrows(IllegalArgumentException::class.java) {
            DocumentParsing.parse(
                bundle,
                listOf("index.txt"),
                python,
                adapter,
                maximumOutputBytes = 2,
            )
        }
    }

    @Test
    fun realParserRetainsHealthyExamplesBesideBlockedLiteralInclude(@TempDir root: Path) {
        val python = System.getProperty("audits.parser.python")
        org.junit.jupiter.api.Assumptions.assumeTrue(
            python != null,
            "Explicit locked parser runtime required for integration",
        )
        val adapter = Path.of(System.getProperty("audits.parser.adapter"))
        val repository = Files.createDirectory(root.resolve("repository"))
        Files.createDirectories(repository.resolve("manual/source"))
        Files.createDirectories(repository.resolve("shared/includes"))
        Files.writeString(repository.resolve("manual/snooty.toml"), "name = 'fixture'\n")
        Files.writeString(
            repository.resolve("manual/source/index.txt"),
            "Title\n=====\n\n.. include:: /includes/good.rst\n\n.. literalinclude:: /includes/bad.rst\n",
        )
        Files.writeString(
            repository.resolve("shared/includes/good.rst"),
            "Preserved explanation.\n",
        )
        Files.createSymbolicLink(
            repository.resolve("manual/source/includes"),
            Path.of("../../shared/includes"),
        )
        Files.createSymbolicLink(
            repository.resolve("shared/includes/bad.rst"),
            Path.of("../../../outside.rst"),
        )
        fun git(vararg args: String): String {
            val builder =
                ProcessBuilder(
                    listOf(
                        "git",
                        "-C",
                        repository.toString(),
                        "-c",
                        "user.name=Fixture",
                        "-c",
                        "user.email=fixture@example.invalid",
                        "-c",
                        "commit.gpgsign=false",
                        "-c",
                        "core.hooksPath=${root.resolve("no-hooks")}",
                    ) + args
                )
            builder
                .environment()
                .keys
                .filter { it.startsWith("GIT_") }
                .toList()
                .forEach { builder.environment().remove(it) }
            builder.environment()["GIT_CONFIG_NOSYSTEM"] = "1"
            builder.environment()["GIT_CONFIG_GLOBAL"] = "/dev/null"
            val process = builder.redirectErrorStream(true).start()
            assertTrue(process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS))
            val output = process.inputStream.readAllBytes().decodeToString()
            assertEquals(0, process.exitValue(), output)
            return output.trim()
        }
        git("init", "--quiet")
        git("add", ".")
        git("commit", "--quiet", "-m", "fixture")
        val request =
            GitSourceRequest(
                id = "fixture",
                uri = repository.toUri().toString(),
                revision = git("rev-parse", "HEAD"),
                projectRoot = "manual",
                label = "8.0",
                role = SourceRole.CURRENT,
                channel = SourceChannel.RELEASED,
                roots = listOf("manual"),
                allowedSymlinkRoots = listOf("shared"),
                maximumFiles = 100,
                maximumFileBytes = 1024,
                maximumTotalBytes = 10000,
                timeoutSeconds = 10,
            )
        val directory = root.resolve("bundle")
        val manifest = SourceBundle.write(repository, request, directory)
        val bundle = SourceBundle.open(directory, manifest.contentSha256)
        val report = DocumentParsing.parse(bundle, listOf("index.txt"), Path.of(python!!), adapter)
        assertEquals(DocumentState.PARTIAL, report.state)
        assertTrue(report.diagnostics.any { it.code == "UNRESOLVED_DEPENDENCY" })
        val page = report.pages.single()
        assertFalse(page.dependenciesComplete)
        assertTrue(page.dependencies.all { it.origin.path == null })
        fun texts(node: DocumentNode): List<String> =
            listOfNotNull(node.text) +
                node.children.flatMap(::texts) +
                node.arguments.flatMap(::texts)
        assertTrue("Preserved explanation." in texts(page.expanded!!))
    }

    @Test
    fun reportCodecRejectsFalseReadyAndUnknownFields(@TempDir root: Path) {
        val bundle = fixture(root)
        val report = response(DocumentParsing.export(bundle, listOf("index.txt")), adapter(root))
        val json = AuditJson.write(report)
        assertEquals(report, AuditJson.documents(json))
        assertThrows(Exception::class.java) {
            AuditJson.documents(json.replace("\"PARTIAL\"", "\"READY\""))
        }
        assertThrows(Exception::class.java) {
            AuditJson.documents(json.replaceFirst("{", "{\"unknown\":true,"))
        }
        assertThrows(Exception::class.java) {
            AuditJson.documents(json.replaceFirst("\"schemaVersion\" : 1", "\"schemaVersion\" : 2"))
        }
    }
}
