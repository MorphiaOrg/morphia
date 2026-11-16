package dev.morphia.audits.sources

import dev.morphia.audits.coverage.AuditJson
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.security.MessageDigest
import java.util.HexFormat

/** A completed manifest is the publication point. Existing destinations are never replaced. */
object SourceBundle {
    const val MANIFEST = "manifest.json"
    private val digest = Regex("[0-9a-f]{64}")
    private val objectId = Regex("[0-9a-f]{40}")

    fun write(repository: Path, request: GitSourceRequest, destination: Path): GitSourceManifest {
        GitSnapshot.validate(request)
        val root = destination.toAbsolutePath().normalize()
        require(root.fileName != null && Files.isDirectory(root.parent)) {
            "Bundle parent must be an explicit existing directory"
        }
        // Atomic reservation: a second writer or a pre-existing corpus directory cannot be
        // replaced.
        Files.createDirectory(root)
        try {
            Files.createDirectory(root.resolve("blobs"))
            val manifest =
                GitSnapshot.capture(repository, request) { sha, bytes ->
                    Files.write(root.resolve("blobs").resolve(sha), bytes, CREATE_NEW)
                }
            val pending = root.resolve("manifest.pending")
            val json = AuditJson.write(manifest).toByteArray(Charsets.UTF_8)
            require(json.size <= 64L * 1024 * 1024) { "Source manifest exceeds 64 MiB" }
            Files.write(pending, json, CREATE_NEW)
            // Verify the exact staged transport, not just the in-memory object that produced it.
            val staged =
                AuditJson.sourceManifest(
                    Files.readAllBytes(pending).decodeToString(throwOnInvalidSequence = true)
                )
            require(staged == manifest) { "Staged manifest differs from the captured source" }
            verify(root, staged, manifest.contentSha256)
            Files.move(pending, root.resolve(MANIFEST), ATOMIC_MOVE)
            return manifest
        } catch (failure: Exception) {
            // This directory was reserved by this invocation and has no accepted manifest.
            runCatching {
                Files.walk(root).use { paths ->
                    paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
                }
            }
            throw failure
        }
    }

    fun open(directory: Path, expectedContentSha256: String): VerifiedSourceBundle {
        require(Files.isDirectory(directory, NOFOLLOW_LINKS)) {
            "Source bundle must be a real directory"
        }
        val manifestPath = directory.resolve(MANIFEST)
        require(Files.isRegularFile(manifestPath, NOFOLLOW_LINKS)) {
            "Source bundle has no completed manifest"
        }
        require(Files.size(manifestPath) <= 64L * 1024 * 1024) { "Source manifest exceeds 64 MiB" }
        val manifest =
            AuditJson.sourceManifest(
                Files.readAllBytes(manifestPath).decodeToString(throwOnInvalidSequence = true)
            )
        verify(directory, manifest, expectedContentSha256)
        return VerifiedSourceBundle(
            directory.toAbsolutePath().normalize(),
            manifest.copy(problems = SourceGraph.inspect(manifest.resources, manifest.request)),
        )
    }

    private fun verify(root: Path, manifest: GitSourceManifest, expected: String) {
        GitSnapshot.validate(manifest.request)
        require(
            manifest.schemaVersion == 1 &&
                objectId.matches(manifest.tree) &&
                manifest.gitVersion.isNotBlank()
        ) {
            "Invalid source manifest provenance"
        }
        require(
            digest.matches(expected) &&
                manifest.contentSha256 == expected &&
                GitSnapshot.contentHash(manifest.request, manifest.tree, manifest.resources) ==
                    expected
        ) {
            "Source manifest does not match the expected content pin"
        }
        require(
            manifest.resources.isNotEmpty() &&
                manifest.resources.map { it.path }.distinct().size == manifest.resources.size
        ) {
            "Source resources must be nonempty and unique"
        }
        require(manifest.resources.size <= manifest.request.maximumFiles) {
            "Source count exceeds its declared budget"
        }
        require(
            manifest.resources.all { it.size in 0..manifest.request.maximumFileBytes } &&
                manifest.resources.sumOf { it.size } <= manifest.request.maximumTotalBytes
        ) {
            "Source sizes exceed declared budgets"
        }
        require((manifest.state == SourceState.READY) == manifest.problems.isEmpty()) {
            "Source state contradicts dependency diagnostics"
        }
        val byPath = manifest.resources.associateBy { it.path }
        manifest.problems.forEach {
            require(
                it.code.isNotBlank() &&
                    it.message.isNotBlank() &&
                    it.path in byPath &&
                    it.chain.isNotEmpty() &&
                    it.chain.all(byPath::containsKey)
            ) {
                "Source diagnostics must identify captured dependency chains"
            }
        }
        require(Files.isDirectory(root.resolve("blobs"), NOFOLLOW_LINKS)) {
            "Source blob store is missing or symlinked"
        }
        manifest.resources.forEach { resource ->
            require(
                SourceGraph.validPath(resource.path) &&
                    objectId.matches(resource.gitObject) &&
                    digest.matches(resource.sha256)
            ) {
                "Invalid source resource identity"
            }
            require((resource.kind == GitResourceKind.SYMLINK) == (resource.linkTarget != null)) {
                "Symlink metadata is inconsistent"
            }
            val blob = root.resolve("blobs").resolve(resource.sha256)
            require(
                Files.isRegularFile(blob, NOFOLLOW_LINKS) && Files.size(blob) == resource.size
            ) {
                "Missing or changed source blob: ${resource.path}"
            }
            val bytes = Files.readAllBytes(blob)
            require(GitSnapshot.digest(bytes) == resource.sha256) {
                "Source blob hash changed: ${resource.path}"
            }
            val gitHash = MessageDigest.getInstance("SHA-1")
            gitHash.update("blob ${bytes.size}\u0000".toByteArray(Charsets.US_ASCII))
            gitHash.update(bytes)
            require(HexFormat.of().formatHex(gitHash.digest()) == resource.gitObject) {
                "Source bytes do not match the pinned Git object"
            }
            if (resource.linkTarget != null)
                require(
                    bytes.decodeToString(throwOnInvalidSequence = true) == resource.linkTarget
                ) {
                    "Symlink text differs from its raw object"
                }
        }
        SourceGraph.validateStructure(manifest.resources)
        val computed = SourceGraph.inspect(manifest.resources, manifest.request)
        fun identity(problem: SourceProblem) = Triple(problem.code, problem.path, problem.chain)
        require(
            computed.map(::identity).toSet() == manifest.problems.map(::identity).toSet() &&
                manifest.problems.map(::identity).distinct().size == manifest.problems.size
        ) {
            "Source diagnostics do not match the raw dependency graph"
        }
        fun covered(path: String, roots: List<String>): Boolean =
            roots.any { it == "." || path == it || path.startsWith("$it/") }
        require(
            manifest.resources.all {
                covered(it.path, manifest.request.roots + manifest.request.allowedSymlinkRoots)
            }
        ) {
            "Unexpected resource outside declared source/dependency roots"
        }
        require(
            manifest.request.roots.all { rootPath ->
                manifest.resources.any { covered(it.path, listOf(rootPath)) }
            }
        ) {
            "A selected source root is missing"
        }
    }
}

data class ResolvedSource(
    val requestedPath: String,
    val canonicalPath: String,
    val linkChain: List<String>,
    val resource: GitResource,
    val bytes: ByteArray,
)

class UnresolvedSource(val problem: SourceProblem) : IllegalArgumentException(problem.message)

/** Only manifest-listed raw objects can be read. Symlinks are virtual metadata, never OS links. */
class VerifiedSourceBundle
internal constructor(private val root: Path, val manifest: GitSourceManifest) {
    private val resources = manifest.resources.associateBy { it.path }

    fun read(path: String): ResolvedSource {
        val chain = mutableListOf<String>()
        fun fail(code: String, message: String): Nothing =
            throw UnresolvedSource(SourceProblem(code, path, chain.toList(), message))
        if (
            !Files.isDirectory(root, NOFOLLOW_LINKS) ||
                !Files.isDirectory(root.resolve("blobs"), NOFOLLOW_LINKS)
        )
            fail("SOURCE_DRIFT", "Source bundle directories changed")
        if (!SourceGraph.validPath(path))
            fail("UNRESOLVED_RESOURCE", "Resource path must stay inside the pinned source")
        var current = path
        while (true) {
            val parts = current.split('/')
            val link =
                parts.indices
                    .mapNotNull { index ->
                        resources[parts.take(index + 1).joinToString("/")]?.takeIf {
                            it.kind == GitResourceKind.SYMLINK
                        }
                    }
                    .firstOrNull()
            if (link == null) break
            if (link.path in chain) {
                chain += link.path
                fail("SOURCE_CYCLE", "Repository symlink dependency cycle")
            }
            if (chain.size >= SourceGraph.MAX_LINK_DEPTH)
                fail(
                    "SOURCE_DEPTH",
                    "Source dependency depth exceeds ${SourceGraph.MAX_LINK_DEPTH} links",
                )
            chain += link.path
            val prefix =
                SourceGraph.target(link.path, link.linkTarget!!)
                    ?: fail(
                        "UNRESOLVED_SYMLINK",
                        "Symlink target is unsupported or leaves its pinned repository",
                    )
            if (manifest.request.allowedSymlinkRoots.none { SourceGraph.within(prefix, it) })
                fail("UNDECLARED_DEPENDENCY", "Symlink target is outside declared dependency roots")
            current =
                (listOf(prefix).filter { it.isNotEmpty() && it != "." } +
                        current.removePrefix(link.path).trimStart('/').takeIf { it.isNotEmpty() })
                    .filterNotNull()
                    .joinToString("/")
            if (!SourceGraph.validPath(current))
                fail("UNRESOLVED_RESOURCE", "Symlink does not resolve to a source file")
        }
        val resource =
            resources[current] ?: fail("UNRESOLVED_RESOURCE", "Pinned resource is absent: $current")
        val blob = root.resolve("blobs").resolve(resource.sha256)
        if (!Files.isRegularFile(blob, NOFOLLOW_LINKS) || Files.size(blob) != resource.size)
            fail("SOURCE_DRIFT", "Pinned source blob disappeared or changed: $current")
        val bytes = Files.readAllBytes(blob)
        if (GitSnapshot.digest(bytes) != resource.sha256)
            fail("SOURCE_DRIFT", "Pinned source blob hash changed: $current")
        return ResolvedSource(path, current, chain.toList(), resource, bytes)
    }
}
