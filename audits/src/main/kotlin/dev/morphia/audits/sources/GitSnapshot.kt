package dev.morphia.audits.sources

import dev.morphia.audits.coverage.AuditJson
import java.net.URI
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat

/**
 * Captures raw immutable objects. The sink receives each distinct blob once; no worktree file is
 * read.
 */
object GitSnapshot {
    private val sha1 = Regex("[0-9a-f]{40}")
    private val ids = Regex("[a-z][a-z0-9-]*")

    fun capture(
        repository: Path,
        request: GitSourceRequest,
        sink: (String, ByteArray) -> Unit,
    ): GitSourceManifest {
        validate(request)
        val resources = sortedMapOf<String, GitResource>()
        val stored = mutableSetOf<String>()
        var total = 0L
        GitObjects(repository, request.timeoutSeconds).use { git ->
            git.commit(request.revision)
            val tree = git.tree(request.revision)
            val listings = mutableMapOf<String, List<GitObjects.Entry>>()
            fun list(root: String): List<GitObjects.Entry> =
                listings.getOrPut(root) { git.entries(request.revision, root) }
            fun within(path: String, root: String): Boolean =
                root == "." || path == root || path.startsWith("$root/")
            fun visit(entry: GitObjects.Entry, chain: List<String>) {
                require(SourceGraph.validPath(entry.path)) {
                    "Nonportable repository path: ${entry.path}"
                }
                if (entry.path in chain) {
                    return
                }
                if (entry.path in resources) return
                require(resources.size < request.maximumFiles) {
                    "Source file-count budget exceeded"
                }
                val bytes = git.blob(entry.objectId, request.maximumFileBytes)
                require(bytes.size.toLong() <= request.maximumTotalBytes - total) {
                    "Source byte budget exceeded"
                }
                total += bytes.size
                val hash = digest(bytes)
                val kind =
                    when (entry.mode) {
                        "100644" -> GitResourceKind.FILE
                        "100755" -> GitResourceKind.EXECUTABLE
                        else -> GitResourceKind.SYMLINK
                    }
                val link =
                    if (kind == GitResourceKind.SYMLINK)
                        bytes.decodeToString(throwOnInvalidSequence = true)
                    else null
                resources[entry.path] =
                    GitResource(entry.path, kind, entry.objectId, hash, bytes.size.toLong(), link)
                if (stored.add(hash)) sink(hash, bytes)
                if (link != null) {
                    val resolved = SourceGraph.target(entry.path, link)
                    if (
                        resolved != null &&
                            chain.size < SourceGraph.MAX_LINK_DEPTH &&
                            request.allowedSymlinkRoots.any { within(resolved, it) }
                    )
                        list(resolved).forEach { visit(it, chain + entry.path) }
                }
            }
            request.roots.sorted().forEach { root ->
                val entries = list(root)
                require(entries.isNotEmpty()) { "Selected source root is absent or empty: $root" }
                entries.forEach { visit(it, emptyList()) }
            }
            require(resources.keys.any { within(it, request.projectRoot) }) {
                "The declared project root has no captured inputs"
            }
            SourceGraph.validateStructure(resources.values.toList())
            git.verifyMetadata()
            val ordered = SourceGraph.inspect(resources.values.toList(), request)
            val canonicalRequest =
                request.copy(
                    roots = request.roots.sorted(),
                    allowedSymlinkRoots = request.allowedSymlinkRoots.sorted(),
                )
            return GitSourceManifest(
                request = canonicalRequest,
                tree = tree,
                gitVersion = git.gitVersion(),
                contentSha256 = contentHash(canonicalRequest, tree, resources.values.toList()),
                state = if (ordered.isEmpty()) SourceState.READY else SourceState.PARTIAL,
                resources = resources.values.toList(),
                problems = ordered,
            )
        }
    }

    internal fun digest(bytes: ByteArray): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))

    internal fun contentHash(
        request: GitSourceRequest,
        tree: String,
        resources: List<GitResource>,
    ): String =
        AuditJson.hash(
            mapOf(
                "schemaVersion" to 1,
                "id" to request.id,
                "label" to request.label,
                "role" to request.role,
                "channel" to request.channel,
                "uri" to request.uri,
                "revision" to request.revision,
                "projectRoot" to request.projectRoot,
                "roots" to request.roots.sorted(),
                "allowedSymlinkRoots" to request.allowedSymlinkRoots.sorted(),
                "tree" to tree,
                "resources" to resources.sortedBy { it.path },
            )
        )

    internal fun validate(request: GitSourceRequest) {
        require(
            request.schemaVersion == 1 && ids.matches(request.id) && sha1.matches(request.revision)
        ) {
            "A named source and full immutable Git commit are required"
        }
        val uri = URI(request.uri)
        require(
            uri.isAbsolute &&
                uri.scheme in setOf("https", "file") &&
                uri.userInfo == null &&
                uri.fragment == null &&
                uri.query == null
        ) {
            "Source URI must be explicit HTTPS or a local fixture URI without credentials"
        }
        require(request.label.isNotBlank() && SourceGraph.validPath(request.projectRoot, true)) {
            "Project root/label must be explicit"
        }
        require(
            request.roots.isNotEmpty() &&
                request.roots.distinct().size == request.roots.size &&
                request.roots.all { SourceGraph.validPath(it, true) }
        ) {
            "Source roots must be unique, explicit repository paths"
        }
        require(
            request.allowedSymlinkRoots.distinct().size == request.allowedSymlinkRoots.size &&
                request.allowedSymlinkRoots.all { SourceGraph.validPath(it, true) }
        ) {
            "Dependency roots must be unique explicit repository paths"
        }
        require(
            request.maximumFiles in 1..100_000 &&
                request.maximumFileBytes in 1..128L * 1024 * 1024 &&
                request.maximumTotalBytes in request.maximumFileBytes..2L * 1024 * 1024 * 1024 &&
                request.timeoutSeconds in 1..600
        ) {
            "Explicit bounded resource limits are required"
        }
    }
}
