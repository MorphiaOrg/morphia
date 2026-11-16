package dev.morphia.audits.offline

import dev.morphia.audits.coverage.*
import dev.morphia.audits.sources.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GitSnapshotTest {
    private data class Fixture(val root: Path, val revision: String)

    private fun git(root: Path, vararg arguments: String): String {
        val builder =
            ProcessBuilder(
                listOf(
                    "git",
                    "-C",
                    root.toString(),
                    "-c",
                    "user.name=Fixture",
                    "-c",
                    "user.email=fixture@example.invalid",
                    "-c",
                    "commit.gpgsign=false",
                    "-c",
                    "core.hooksPath=${root.resolve("empty-hooks")}",
                ) + arguments
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
        assertTrue(process.waitFor(15, TimeUnit.SECONDS), "Fixture Git timed out")
        val output = process.inputStream.readAllBytes().decodeToString()
        assertEquals(0, process.exitValue(), output)
        return output.trim()
    }

    private fun fixture(root: Path, links: Boolean = true): Fixture {
        Files.createDirectories(root.resolve("manual/source"))
        Files.createDirectories(root.resolve("examples"))
        root.resolve("manual/snooty.toml").writeText("name = 'fixture'\n")
        root
            .resolve("manual/source/median.txt")
            .writeText("Median Ratings\n==============\n\n.. versionadded:: 7.0\n")
        root
            .resolve("examples/median.js")
            .writeBytes(
                "// Café\r\ndb.movies.aggregate([{ ${'$'}group: { _id: null } }]);\r\n"
                    .toByteArray()
            )
        if (links)
            Files.createSymbolicLink(
                root.resolve("manual/source/tested"),
                Path.of("../../examples"),
            )
        git(root, "init", "--quiet")
        git(root, "add", ".")
        git(root, "commit", "--quiet", "-m", "fixture")
        return Fixture(root, git(root, "rev-parse", "HEAD"))
    }

    private fun request(fixture: Fixture): GitSourceRequest =
        GitSourceRequest(
            id = "manual",
            uri = fixture.root.toUri().toString(),
            revision = fixture.revision,
            projectRoot = "manual",
            label = "7.0",
            role = SourceRole.CURRENT,
            channel = SourceChannel.RELEASED,
            roots = listOf("manual"),
            allowedSymlinkRoots = listOf("examples"),
            maximumFiles = 100,
            maximumFileBytes = 1024 * 1024,
            maximumTotalBytes = 4 * 1024 * 1024,
            timeoutSeconds = 20,
        )

    private fun capture(
        fixture: Fixture,
        request: GitSourceRequest = request(fixture),
    ): Pair<GitSourceManifest, Map<String, ByteArray>> {
        val blobs = mutableMapOf<String, ByteArray>()
        val manifest =
            GitSnapshot.capture(fixture.root, request) { hash, bytes ->
                assertNull(blobs.put(hash, bytes), "Duplicate blob callback")
            }
        return manifest to blobs
    }

    @Test
    fun capturesRawPinnedBytesAndRepositorySymlinkClosure(@TempDir root: Path) {
        val fixture = fixture(root)
        val (manifest, blobs) = capture(fixture)
        assertEquals(SourceState.READY, manifest.state, manifest.problems.toString())
        assertEquals(
            setOf(
                "manual/snooty.toml",
                "manual/source/median.txt",
                "manual/source/tested",
                "examples/median.js",
            ),
            manifest.resources.map { it.path }.toSet(),
        )
        val link = manifest.resources.single { it.kind == GitResourceKind.SYMLINK }
        assertEquals("../../examples", link.linkTarget)
        assertArrayEquals("../../examples".toByteArray(), blobs.getValue(link.sha256))
        val example = manifest.resources.single { it.path == "examples/median.js" }
        assertArrayEquals(root.resolve(example.path).readBytes(), blobs.getValue(example.sha256))
        assertTrue(blobs.getValue(example.sha256).decodeToString().contains("Café\r\n"))
        assertTrue(manifest.tree.matches(Regex("[0-9a-f]{40}")))
        assertTrue(manifest.contentSha256.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun dirtyWorktreesAndCheckoutFiltersCannotChangeTheSnapshot(@TempDir root: Path) {
        val fixture = fixture(root)
        val baseline = capture(fixture)
        root.resolve("manual/source/median.txt").writeText("Dirty text must not be captured")
        root.resolve("manual/source/untracked.txt").writeText("Untracked data")
        root.resolve(".gitattributes").writeText("*.txt filter=unexpected\n")
        val marker = root.resolve("filter-executed")
        git(root, "config", "filter.unexpected.smudge", "touch $marker")
        val changed = capture(fixture)
        assertEquals(AuditJson.write(baseline.first), AuditJson.write(changed.first))
        baseline.second.forEach { (hash, bytes) ->
            assertArrayEquals(bytes, changed.second.getValue(hash))
        }
        assertFalse(marker.exists())
    }

    @Test
    fun undeclaredAndMissingSymlinkTargetsRemainExplicitlyPartial(@TempDir root: Path) {
        val fixture = fixture(root)
        val undeclared =
            capture(fixture, request(fixture).copy(allowedSymlinkRoots = emptyList())).first
        assertEquals(SourceState.PARTIAL, undeclared.state)
        assertEquals("UNDECLARED_DEPENDENCY", undeclared.problems.single().code)
        assertFalse(undeclared.resources.any { it.path.startsWith("examples/") })
        root.resolve("manual/source/tested").deleteExisting()
        Files.createSymbolicLink(root.resolve("manual/source/tested"), Path.of("../../absent"))
        git(root, "add", ".")
        git(root, "commit", "--quiet", "-m", "missing dependency")
        val missing = fixture.copy(revision = git(root, "rev-parse", "HEAD"))
        val result =
            capture(missing, request(missing).copy(allowedSymlinkRoots = listOf("absent"))).first
        assertEquals(SourceState.PARTIAL, result.state)
        assertEquals("UNRESOLVED_SYMLINK", result.problems.single().code)
        assertEquals(listOf("manual/source/tested"), result.problems.single().chain)
    }

    @Test
    fun symlinkCyclesAndEscapesNeverReadOutsideObjects(@TempDir root: Path) {
        val fixture = fixture(root, links = false)
        Files.createSymbolicLink(root.resolve("manual/source/a"), Path.of("b"))
        Files.createSymbolicLink(root.resolve("manual/source/b"), Path.of("a"))
        Files.createSymbolicLink(
            root.resolve("manual/source/outside"),
            Path.of("../../../../outside"),
        )
        git(root, "add", ".")
        git(root, "commit", "--quiet", "-m", "link boundaries")
        val changed = fixture.copy(revision = git(root, "rev-parse", "HEAD"))
        val result =
            capture(changed, request(changed).copy(allowedSymlinkRoots = listOf("manual/source")))
                .first
        assertEquals(SourceState.PARTIAL, result.state)
        assertEquals(
            setOf("SOURCE_CYCLE", "UNRESOLVED_SYMLINK"),
            result.problems.map { it.code }.toSet(),
        )
        assertTrue(result.problems.single { it.code == "SOURCE_CYCLE" }.chain.size >= 3)
        assertTrue(result.resources.all { it.path.startsWith("manual/") })
    }

    @Test
    fun selectionOrderAndExecutionBudgetsDoNotChangeContentIdentity(@TempDir root: Path) {
        val fixture = fixture(root)
        val one =
            request(fixture)
                .copy(
                    roots = listOf("manual", "examples"),
                    allowedSymlinkRoots = listOf("manual/source", "examples"),
                )
        val two =
            one.copy(
                roots = one.roots.reversed(),
                allowedSymlinkRoots = one.allowedSymlinkRoots.reversed(),
            )
        assertEquals(
            AuditJson.write(capture(fixture, one).first),
            AuditJson.write(capture(fixture, two).first),
        )
        assertEquals(
            capture(fixture, one).first.contentSha256,
            capture(fixture, one.copy(timeoutSeconds = 30, maximumFiles = 200)).first.contentSha256,
        )
    }

    @Test
    fun missingRootsMutablePinsAndExplicitBudgetsFail(@TempDir root: Path) {
        val fixture = fixture(root)
        val invalid =
            listOf(
                request(fixture).copy(revision = "HEAD"),
                request(fixture).copy(roots = listOf("../escape")),
                request(fixture).copy(roots = emptyList()),
                request(fixture).copy(uri = "https://secret@example.invalid/repo"),
                request(fixture).copy(maximumFiles = 0),
                request(fixture).copy(timeoutSeconds = 0),
            )
        invalid.forEach {
            assertThrows(IllegalArgumentException::class.java) { capture(fixture, it) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            capture(fixture, request(fixture).copy(roots = listOf("missing")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            capture(fixture, request(fixture).copy(maximumFiles = 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            capture(fixture, request(fixture).copy(maximumFileBytes = 1))
        }
    }

    @Test
    fun bundlesResolveVirtualAliasesAndRetainCanonicalProvenance(@TempDir root: Path) {
        val repository = root.resolve("repository").createDirectory()
        val fixture = fixture(repository)
        val directory = root.resolve("bundle")
        val manifest = SourceBundle.write(repository, request(fixture), directory)
        assertEquals(manifest, AuditJson.sourceManifest(AuditJson.write(manifest)))
        assertEquals(request(fixture), AuditJson.sourceRequest(AuditJson.write(request(fixture))))
        val bundle = SourceBundle.open(directory, manifest.contentSha256)
        val resolved = bundle.read("manual/source/tested/median.js")
        assertEquals("examples/median.js", resolved.canonicalPath)
        assertEquals(listOf("manual/source/tested"), resolved.linkChain)
        assertArrayEquals(repository.resolve("examples/median.js").readBytes(), resolved.bytes)
        assertTrue(directory.resolve("manifest.json").isRegularFile())
        assertFalse(directory.resolve("manual").exists())
        assertThrows(UnresolvedSource::class.java) { bundle.read("../outside") }
        assertThrows(UnresolvedSource::class.java) { bundle.read("not-captured.txt") }
        assertThrows(IllegalArgumentException::class.java) {
            SourceBundle.open(directory, "0".repeat(64))
        }
    }

    @Test
    fun existingDestinationsAndFailedCandidatesNeverReplaceAcceptedInputs(@TempDir root: Path) {
        val repository = root.resolve("repository").createDirectory()
        val fixture = fixture(repository)
        val existing = root.resolve("existing").createDirectory()
        existing.resolve("curated.json").writeText("handwritten")
        assertThrows(java.nio.file.FileAlreadyExistsException::class.java) {
            SourceBundle.write(repository, request(fixture), existing)
        }
        assertEquals("handwritten", existing.resolve("curated.json").readText())
        val failed = root.resolve("failed")
        assertThrows(IllegalArgumentException::class.java) {
            SourceBundle.write(repository, request(fixture).copy(maximumFiles = 1), failed)
        }
        assertFalse(failed.exists())
        val incomplete = root.resolve("incomplete").createDirectory()
        assertThrows(IllegalArgumentException::class.java) {
            SourceBundle.open(incomplete, "a".repeat(64))
        }
    }

    @Test
    fun changedBlobsAndForgedDependencyHealthCannotBecomeValidSources(@TempDir root: Path) {
        val repository = root.resolve("repository").createDirectory()
        val fixture = fixture(repository)
        val directory = root.resolve("bundle")
        val manifest =
            SourceBundle.write(
                repository,
                request(fixture)
                    .copy(allowedSymlinkRoots = emptyList(), roots = listOf("manual", "examples")),
                directory,
            )
        assertEquals(SourceState.PARTIAL, manifest.state)
        assertThrows(UnresolvedSource::class.java) {
            SourceBundle.open(directory, manifest.contentSha256)
                .read("manual/source/tested/median.js")
        }
        val forged = manifest.copy(state = SourceState.READY, problems = emptyList())
        directory.resolve("manifest.json").writeText(AuditJson.write(forged))
        assertThrows(IllegalArgumentException::class.java) {
            SourceBundle.open(directory, manifest.contentSha256)
        }
        directory.resolve("manifest.json").writeText(AuditJson.write(manifest))
        val bundle = SourceBundle.open(directory, manifest.contentSha256)
        val resource = manifest.resources.single { it.path == "examples/median.js" }
        directory
            .resolve("blobs")
            .resolve(resource.sha256)
            .writeBytes(ByteArray(resource.size.toInt()) { 'x'.code.toByte() })
        assertThrows(IllegalArgumentException::class.java) {
            SourceBundle.open(directory, manifest.contentSha256)
        }
        assertThrows(UnresolvedSource::class.java) { bundle.read("examples/median.js") }
    }

    @Test
    fun bundleReadersRejectFilesystemLinksAndNonStrictJson(@TempDir root: Path) {
        val repository = root.resolve("repository").createDirectory()
        val fixture = fixture(repository)
        val directory = root.resolve("bundle")
        val manifest = SourceBundle.write(repository, request(fixture), directory)
        val resource = manifest.resources.single { it.path == "examples/median.js" }
        val path = directory.resolve("blobs").resolve(resource.sha256)
        path.deleteExisting()
        Files.createSymbolicLink(path, repository.resolve(resource.path))
        assertThrows(IllegalArgumentException::class.java) {
            SourceBundle.open(directory, manifest.contentSha256)
        }
        assertThrows(Exception::class.java) {
            AuditJson.sourceRequest(
                AuditJson.write(request(fixture))
                    .replace("\"roots\" : [ \"manual\" ]", "\"roots\" : null")
            )
        }
        assertThrows(Exception::class.java) { AuditJson.sourceManifest("{\"resources\":[]}") }
    }

    @Test
    fun explicitFilteredAcquisitionHydratesOnlyPinnedInputsWithoutCheckout(@TempDir root: Path) {
        val repository = root.resolve("origin").createDirectory()
        val fixture = fixture(repository)
        git(repository, "config", "uploadpack.allowFilter", "true")
        git(repository, "config", "uploadpack.allowAnySHA1InWant", "true")
        val cache = root.resolve("cache")
        GitAcquisition.fetch(request(fixture), cache)
        assertTrue(cache.resolve("HEAD").isRegularFile())
        assertFalse(cache.resolve("manual").exists())
        val original = capture(fixture).first
        val manifest = SourceBundle.write(cache, request(fixture), root.resolve("bundle"))
        assertEquals(original.contentSha256, manifest.contentSha256)
        assertArrayEquals(
            repository.resolve("examples/median.js").readBytes(),
            SourceBundle.open(root.resolve("bundle"), manifest.contentSha256)
                .read("manual/source/tested/median.js")
                .bytes,
        )
        assertThrows(java.nio.file.FileAlreadyExistsException::class.java) {
            GitAcquisition.fetch(request(fixture), cache)
        }
    }

    @Test
    fun explicitAcquisitionCliKeepsOrdinaryReadsOfflineAndPreservesInputs(@TempDir root: Path) {
        val repository = root.resolve("origin").createDirectory()
        val fixture = fixture(repository)
        val plan = root.resolve("request.json")
        val json = AuditJson.write(request(fixture).copy(label = "Manual Café"))
        plan.writeText(json)
        fun run(vararg args: String): Pair<Int, SourceAcquisitionReport> {
            val process =
                ProcessBuilder(
                        listOf(
                            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                            "-Dfile.encoding=US-ASCII",
                            "-cp",
                            System.getProperty("java.class.path"),
                            "dev.morphia.audits.sources.SourcesMainKt",
                        ) + args
                    )
                    .start()
            val output =
                process.inputStream.readAllBytes().decodeToString(throwOnInvalidSequence = true)
            val errors = process.errorStream.readAllBytes().decodeToString()
            assertEquals("", errors)
            return process.waitFor() to AuditJson.sourceAcquisition(output)
        }
        val directory = root.resolve("bundle")
        val result =
            run(plan.toString(), directory.toString(), "--repository", repository.toString())
        assertEquals(0, result.first, result.second.problems.toString())
        assertEquals("Manual Café", result.second.manifest!!.request.label)
        assertEquals(json, plan.readText())
        val repeated =
            run(plan.toString(), directory.toString(), "--repository", repository.toString())
        assertEquals(2, repeated.first)
        assertEquals(AcquisitionState.INVALID, repeated.second.state)
        assertEquals(
            result.second.manifest,
            SourceBundle.open(directory, result.second.manifest!!.contentSha256).manifest,
        )
        val partialPlan = root.resolve("partial.json")
        partialPlan.writeText(
            AuditJson.write(request(fixture).copy(allowedSymlinkRoots = emptyList()))
        )
        assertEquals(
            3,
            run(
                    partialPlan.toString(),
                    root.resolve("partial").toString(),
                    "--repository",
                    repository.toString(),
                )
                .first,
        )
        plan.writeText("{}")
        assertEquals(
            2,
            run(
                    plan.toString(),
                    root.resolve("invalid").toString(),
                    "--repository",
                    repository.toString(),
                )
                .first,
        )
        assertFalse(root.resolve("invalid").exists())
    }

    @Test
    fun embeddedParentNavigationCannotReturnTheWrongFile(@TempDir root: Path) {
        val repository = root.resolve("origin").createDirectory()
        val fixture = fixture(repository, links = false)
        repository.resolve("p/q").createDirectories()
        repository.resolve("p/q/z").writeText("z")
        repository.resolve("p/y").writeText("right")
        repository.resolve("manual/source/y").writeText("wrong")
        repository.resolve("manual/source/file.txt").writeText("plain")
        Files.createSymbolicLink(repository.resolve("manual/source/alias"), Path.of("../../p/q"))
        listOf("alias/../y", "missing/../y", "file.txt/../y", "y/.").forEachIndexed { index, target
            ->
            Files.createSymbolicLink(
                repository.resolve("manual/source/link$index"),
                Path.of(target),
            )
        }
        git(repository, "add", ".")
        git(repository, "commit", "--quiet", "-m", "navigation fixture")
        val revised = fixture.copy(revision = git(repository, "rev-parse", "HEAD"))
        val manifest =
            SourceBundle.write(
                repository,
                request(revised).copy(allowedSymlinkRoots = listOf("manual", "p")),
                root.resolve("bundle"),
            )
        assertEquals(SourceState.PARTIAL, manifest.state)
        val bundle = SourceBundle.open(root.resolve("bundle"), manifest.contentSha256)
        repeat(4) { index ->
            assertTrue(
                manifest.problems.any {
                    it.path == "manual/source/link$index" && it.code == "UNRESOLVED_SYMLINK"
                }
            )
            assertThrows(UnresolvedSource::class.java) { bundle.read("manual/source/link$index") }
        }
    }

    @Test
    fun sourceInterpretationMetadataIsBoundToTheExpectedPin(@TempDir root: Path) {
        val repository = root.resolve("origin").createDirectory()
        val fixture = fixture(repository)
        val directory = root.resolve("bundle")
        val manifest = SourceBundle.write(repository, request(fixture), directory)
        listOf(
                manifest.request.copy(id = "changed"),
                manifest.request.copy(label = "different label"),
                manifest.request.copy(role = SourceRole.HISTORICAL),
                manifest.request.copy(channel = SourceChannel.UPCOMING),
            )
            .forEach { changed ->
                directory
                    .resolve("manifest.json")
                    .writeText(AuditJson.write(manifest.copy(request = changed)))
                assertThrows(IllegalArgumentException::class.java) {
                    SourceBundle.open(directory, manifest.contentSha256)
                }
            }
    }

    @Test
    fun corruptedCommitAndTreeObjectsCannotClaimThePinnedRevision(@TempDir root: Path) {
        for (kind in listOf("tree", "commit")) {
            val repository = root.resolve(kind).createDirectory()
            val fixture = fixture(repository)
            val metadata =
                if (kind == "tree")
                    git(repository, "rev-parse", fixture.revision + ":manual/source")
                else fixture.revision
            val loose =
                repository
                    .resolve(".git/objects")
                    .resolve(metadata.take(2))
                    .resolve(metadata.drop(2))
            val bytes =
                java.util.zip.InflaterInputStream(Files.newInputStream(loose)).use {
                    it.readAllBytes()
                }
            if (kind == "tree") {
                val replacement = repository.resolve("replacement.txt")
                replacement.writeText("tampered-but-valid-blob")
                val objectId = git(repository, "hash-object", "-w", replacement.toString())
                val marker = ("median.txt" + 0.toChar()).toByteArray()
                val start =
                    (0..bytes.size - marker.size).first {
                        bytes.copyOfRange(it, it + marker.size).contentEquals(marker)
                    } + marker.size
                java.util.HexFormat.of().parseHex(objectId).copyInto(bytes, start)
            } else {
                val marker = "author Fixture".toByteArray()
                val start =
                    (0..bytes.size - marker.size).first {
                        bytes.copyOfRange(it, it + marker.size).contentEquals(marker)
                    }
                bytes[start + "author ".length] = 'X'.code.toByte()
            }
            assertTrue(loose.toFile().setWritable(true))
            java.util.zip.DeflaterOutputStream(Files.newOutputStream(loose)).use { it.write(bytes) }
            if (kind == "tree")
                assertEquals(
                    "tampered-but-valid-blob",
                    git(repository, "show", fixture.revision + ":manual/source/median.txt"),
                )
            assertThrows(IllegalArgumentException::class.java) { capture(fixture) }
        }
    }

    @Test
    fun subprocessBudgetsAndReportStatesFailClosed(@TempDir root: Path) {
        val fixture = fixture(root)
        val output =
            assertThrows(Exception::class.java) {
                GitCommands.run(root, listOf("--version"), 5, maximumOutput = 1)
            }
        assertTrue(output.message.orEmpty().contains("output", ignoreCase = true))
        val start = System.nanoTime()
        assertThrows(Exception::class.java) {
            GitCommands.run(root, listOf("-c", "alias.auditPause=!sleep 5", "auditPause"), 1)
        }
        assertTrue(
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 4500,
            "Timeout must terminate the child process tree",
        )
        assertThrows(Exception::class.java) {
            AuditJson.sourceAcquisition(
                "{\"schemaVersion\":1,\"state\":\"READY\",\"manifest\":null,\"problems\":[]}"
            )
        }
        assertThrows(Exception::class.java) {
            AuditJson.sourceAcquisition(
                "{\"schemaVersion\":1,\"state\":\"INVALID\",\"manifest\":null,\"problems\":[]}"
            )
        }
        assertEquals(SourceState.READY, capture(fixture).first.state)
    }

    @Test
    fun malformedFileAndDirectoryOverlapsCannotPublishAHealthyBundle(@TempDir root: Path) {
        val repository = root.resolve("origin").createDirectory()
        val fixture = fixture(repository)
        val directory = root.resolve("bundle")
        val manifest = SourceBundle.write(repository, request(fixture), directory)
        val data = manifest.resources.single { it.path == "examples/median.js" }
        for (parent in listOf("manual/source/tested", "examples/median.js")) {
            val resources = manifest.resources + data.copy(path = "$parent/shadow")
            val hash = GitSnapshot.contentHash(manifest.request, manifest.tree, resources)
            directory
                .resolve("manifest.json")
                .writeText(
                    AuditJson.write(manifest.copy(resources = resources, contentSha256 = hash))
                )
            assertThrows(IllegalArgumentException::class.java) {
                SourceBundle.open(directory, hash)
            }
        }
    }
}
