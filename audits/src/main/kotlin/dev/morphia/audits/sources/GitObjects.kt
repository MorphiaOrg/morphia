package dev.morphia.audits.sources

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat

/** Raw Git object reads. No checkout, filters, hooks, replacement objects or lazy network fetch. */
internal class GitObjects(private val repository: Path, private val timeoutSeconds: Long) :
    AutoCloseable {
    data class Entry(val path: String, val mode: String, val objectId: String)

    private val session = GitObjectSession.open(repository, timeoutSeconds)
    private val input = session.input
    private val output = session.output

    fun gitVersion(): String =
        command(listOf("--version")).decodeToString(throwOnInvalidSequence = true).trim()

    private var pinnedRevision: String? = null
    private var rootTree: String? = null
    private val directories = mutableMapOf<String, String>()
    private val verifiedTrees = mutableSetOf<String>()

    fun commit(revision: String): String {
        require(pinnedRevision == null || pinnedRevision == revision) {
            "One immutable revision per object session is required"
        }
        if (pinnedRevision == revision) return revision
        val resolved =
            command(listOf("rev-parse", "--verify", "$revision^{commit}"))
                .decodeToString(throwOnInvalidSequence = true)
                .trim()
        require(resolved == revision) { "Expected a full commit object, not a mutable ref or tag" }
        val raw = readObject(revision, "commit", 8L * 1024 * 1024)
        val firstEnd = raw.indexOf('\n'.code.toByte())
        require(firstEnd in 1..100) { "Invalid pinned commit header" }
        val first = raw.copyOfRange(0, firstEnd).decodeToString(throwOnInvalidSequence = true)
        require(first.matches(Regex("tree [0-9a-f]{40}"))) {
            "Pinned commit has no canonical tree header"
        }
        val tree = first.removePrefix("tree ")
        readObject(tree, "tree", 64L * 1024 * 1024)
        pinnedRevision = revision
        rootTree = tree
        directories[""] = tree
        verifiedTrees += tree
        return revision
    }

    fun tree(revision: String): String {
        commit(revision)
        return rootTree!!
    }

    private fun directory(revision: String, path: String): String {
        commit(revision)
        directories[path]?.let {
            return it
        }
        require(SourceGraph.validPath(path)) { "Invalid pinned directory path" }
        directory(revision, path.substringBeforeLast('/', ""))
        val id =
            command(listOf("rev-parse", "--verify", "$revision:$path"))
                .decodeToString(throwOnInvalidSequence = true)
                .trim()
        require(id.matches(Regex("[0-9a-f]{40}"))) { "Invalid pinned directory object" }
        if (id !in verifiedTrees) {
            readObject(id, "tree", 64L * 1024 * 1024)
            verifiedTrees += id
        }
        directories[path] = id
        return id
    }

    fun verifyMetadata() {
        readObject(requireNotNull(pinnedRevision), "commit", 8L * 1024 * 1024)
        verifiedTrees.sorted().forEach { readObject(it, "tree", 64L * 1024 * 1024) }
    }

    fun entries(revision: String, root: String): List<Entry> {
        commit(revision)
        val bytes = command(listOf("ls-tree", "-r", "-z", "--full-tree", revision, "--", root))
        val text = bytes.decodeToString(throwOnInvalidSequence = true)
        require(text.isEmpty() || text.endsWith('\u0000')) { "Truncated Git tree listing" }
        return text
            .split('\u0000')
            .filter(String::isNotEmpty)
            .map { record ->
                val parts = record.split('\t', limit = 2)
                require(parts.size == 2) { "Malformed Git tree record" }
                val metadata = parts[0].split(' ')
                require(metadata.size == 3 && metadata[2].matches(Regex("[0-9a-f]{40}"))) {
                    "Malformed Git tree identity"
                }
                require(metadata[1] == "blob") {
                    "Unsupported Git resource ${parts[1]} (${metadata[1]}); submodules need separate source pins"
                }
                require(metadata[0] in setOf("100644", "100755", "120000")) {
                    "Unsupported Git resource mode ${metadata[0]}"
                }
                require(SourceGraph.validPath(parts[1])) { "Unsupported source path" }
                directory(revision, parts[1].substringBeforeLast('/', ""))
                Entry(parts[1], metadata[0], metadata[2])
            }
            .also { entries ->
                require(entries.map { it.path }.distinct().size == entries.size) {
                    "Duplicate paths in a pinned Git tree"
                }
            }
    }

    fun blob(objectId: String, maximumBytes: Long): ByteArray =
        readObject(objectId, "blob", maximumBytes)

    private fun readObject(objectId: String, type: String, maximumBytes: Long): ByteArray {
        require(
            objectId.matches(Regex("[0-9a-f]{40}")) && maximumBytes in 0..Int.MAX_VALUE.toLong()
        )
        input.write((objectId + "\n").toByteArray(Charsets.US_ASCII))
        input.flush()
        val header = readHeader().split(' ')
        require(header.size == 3 && header[0] == objectId && header[1] == type) {
            "Git $type unavailable offline: $objectId"
        }
        val size = header[2].toLongOrNull() ?: error("Invalid Git blob size")
        require(size in 0..maximumBytes) {
            "Git blob exceeds explicit resource limit: $objectId ($size bytes)"
        }
        val bytes = output.readNBytes(size.toInt())
        require(bytes.size.toLong() == size && output.read() == '\n'.code) {
            "Truncated Git blob: $objectId"
        }
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update("$type $size\u0000".toByteArray(Charsets.US_ASCII))
        digest.update(bytes)
        require(HexFormat.of().formatHex(digest.digest()) == objectId) {
            "Git object content does not match its identity"
        }
        return bytes
    }

    private fun readHeader(): String {
        val header = StringBuilder()
        while (header.length < 160) {
            val next = output.read()
            require(next >= 0) { "Git object stream ended: ${diagnostics()}" }
            if (next == '\n'.code) return header.toString()
            require(next in 32..126) { "Invalid Git object header" }
            header.append(next.toChar())
        }
        error("Overlong Git object header")
    }

    private fun command(arguments: List<String>): ByteArray =
        GitCommands.run(repository, arguments, timeoutSeconds)

    private fun diagnostics(): String =
        Files.newInputStream(session.errors).use { it.readNBytes(4096).decodeToString() }

    override fun close() = session.close()
}
