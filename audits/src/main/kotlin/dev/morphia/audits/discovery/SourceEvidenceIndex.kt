package dev.morphia.audits.discovery

import dev.morphia.audits.coverage.AuditJson
import dev.morphia.audits.coverage.EvidenceKind
import dev.morphia.audits.coverage.SourceEvidence
import dev.morphia.audits.documents.DocumentNode
import dev.morphia.audits.sources.VerifiedSourceBundle
import java.security.MessageDigest
import java.util.HexFormat

/** Raw source bytes establish evidence; upstream positions are only lookup hints. */
internal class SourceEvidenceIndex(private val bundle: VerifiedSourceBundle) {
    private data class Lines(val bytes: ByteArray, val starts: List<Int>, val text: List<String>) {
        fun span(first: Int, last: Int): ByteArray =
            bytes.copyOfRange(starts[first], starts.getOrElse(last + 1) { bytes.size })
    }

    private val resources = mutableMapOf<String, Lines>()
    private val evidence = linkedMapOf<String, SourceEvidence>()

    fun file(node: DocumentNode, kind: EvidenceKind, anchor: String): SourceEvidence? =
        this.node(node, kind, anchor, wholeFile = true) { true }

    /** Snooty reports a section heading at its underline, so the hinted raw line must be one. */
    fun heading(node: DocumentNode, kind: EvidenceKind, anchor: String): SourceEvidence? =
        this.node(node, kind, anchor, wholeFile = true, paragraph = false) { adornment.matches(it) }

    private val adornment = Regex("^\\s*([!-/:-@\\[-`{-~])\\1+\\s*$")

    fun membership(
        node: DocumentNode,
        token: String,
        roles: List<String>,
        anchor: String,
    ): SourceEvidence? {
        val end = "(?![A-Za-z0-9_\\[\\].])"
        val literal = Regex.escape(token) + end
        val references =
            Regex(
                ":(?:mongodb:)?(?:${roles.joinToString("|") { Regex.escape(it) }}):`[^`\\r\\n]*$literal[^`]*`"
            )
        val bare = Regex("^\\s*\\*\\s*-\\s*(?:``)?$literal(?:``)?\\s*$")
        return this.node(node, EvidenceKind.DEFINITION, anchor) { raw ->
            val normalized = raw.replace("\\$", "$").replace("\\<", "<").replace("\\>", ">")
            references.containsMatchIn(normalized) || bare.matches(normalized)
        }
    }

    fun directiveArgument(node: DocumentNode, name: String): String? {
        val path = node.origin.path ?: return null
        val lines =
            bundle
                .read(path)
                .bytes
                .decodeToString(throwOnInvalidSequence = true)
                .lineSequence()
                .toList()
        val directive = Regex("^\\s*\\.\\. (?:mongodb:)?${Regex.escape(name)}::\\s*(.*)$")
        node.origin.reportedLine?.let { line ->
            lines.getOrNull(line - 1)?.let {
                directive.matchEntire(it)?.let { match ->
                    return match.groupValues[1].trimEnd()
                }
            }
        }
        val rendered = node.arguments.joinToString("") { DocumentFacts.text(it) }
        val candidates =
            lines
                .mapNotNull { directive.matchEntire(it)?.groupValues?.get(1)?.trimEnd() }
                .filter { it == rendered }
                .distinct()
        return candidates.singleOrNull()
    }

    fun items(): List<SourceEvidence> = evidence.values.sortedBy { it.id }

    /**
     * A verified unique raw line is precise evidence even for generated YAML. If several raw lines
     * match, retain honest whole-file evidence rather than inventing a generated position.
     */
    fun node(
        node: DocumentNode,
        kind: EvidenceKind,
        anchor: String,
        wholeFile: Boolean = false,
        paragraph: Boolean = true,
        matches: (String) -> Boolean,
    ): SourceEvidence? {
        val origin = node.origin
        val path = origin.path ?: return null
        val source = bundle.read(path)
        require(source.resource.sha256 == origin.sha256) { "Evidence source drift: $path" }
        val lines =
            resources.getOrPut(path) {
                val bytes = source.bytes
                val starts = mutableListOf(0)
                bytes.indices.forEach { i ->
                    if (bytes[i] == '\n'.code.toByte() && i + 1 < bytes.size) starts += i + 1
                }
                val text =
                    starts.indices.map { i ->
                        bytes
                            .copyOfRange(starts[i], starts.getOrElse(i + 1) { bytes.size })
                            .decodeToString(throwOnInvalidSequence = true)
                            .trimEnd('\r', '\n')
                    }
                Lines(bytes, starts, text)
            }
        val candidates = lines.text.indices.filter { matches(lines.text[it]) }
        if (candidates.isEmpty()) return null
        val hint = origin.reportedLine?.minus(1)
        // Snooty reports inline content at its paragraph's first line, so a parser-reported hint
        // may only be satisfied within that raw paragraph, never by a match elsewhere in the file.
        val located =
            if (hint == null || hint !in lines.text.indices) null
            else if (hint in candidates) hint
            else if (!paragraph) null
            else
                (hint until lines.text.size)
                    .takeWhile { it == hint || lines.text[it].isNotBlank() }
                    .filter { it in candidates }
                    .singleOrNull()
        if (
            origin.precision == dev.morphia.audits.documents.DocumentPrecision.PARSER_REPORTED &&
                located == null
        )
            return null
        val chosen = if (wholeFile) null else located ?: candidates.singleOrNull()
        val first = chosen ?: 0
        val last = chosen ?: lines.starts.lastIndex
        val value =
            SourceEvidence(
                id = "pending",
                source = bundle.manifest.request.id,
                path = path,
                anchor = anchor,
                startLine = (first + 1).toLong(),
                endLine = (last + 1).toLong(),
                textSha256 =
                    HexFormat.of()
                        .formatHex(
                            MessageDigest.getInstance("SHA-256").digest(lines.span(first, last))
                        ),
                kind = kind,
            )
        val id = "evidence-" + AuditJson.hash(value)
        val pinned = value.copy(id = id)
        require(evidence.putIfAbsent(id, pinned)?.let { it == pinned } != false)
        return pinned
    }
}
