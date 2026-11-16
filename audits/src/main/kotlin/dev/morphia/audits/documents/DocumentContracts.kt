package dev.morphia.audits.documents

import dev.morphia.audits.sources.GitResourceKind
import dev.morphia.audits.sources.SourceProblem

/** Only the coordinator exports snapshots. No ambient paths or source fetch are accepted. */
data class DocumentInput(
    val schemaVersion: Int = 1,
    val sourceSha256: String,
    val projectRoot: String,
    val pages: List<String>,
    val resources: List<DocumentResource>,
    val sourceProblems: List<SourceProblem>,
)

data class DocumentResource(
    val path: String,
    val kind: GitResourceKind,
    val sha256: String,
    val contentsBase64: String,
    val linkTarget: String?,
)

enum class DocumentPrecision {
    PARSER_REPORTED,
    GENERATED_OR_UNKNOWN,
}

/**
 * path is a canonical bundle-relative resource; includeChain contains bundle-relative requested
 * paths (including virtual generated paths). Reported lines are parser hints, not external spans.
 */
data class DocumentOrigin(
    val path: String?,
    val sha256: String?,
    val reportedLine: Int?,
    val precision: DocumentPrecision,
    val includeChain: List<String>,
)

/** Stable owned tree; unknown node kinds and extra attributes survive compatibility probes. */
data class DocumentNode(
    val kind: String,
    val origin: DocumentOrigin,
    val text: String?,
    val name: String?,
    val domain: String?,
    val identifiers: List<String>,
    /** Values are canonical JSON, preserving booleans/numbers/structured options losslessly. */
    val options: Map<String, String>,
    val attributes: Map<String, String>,
    val arguments: List<DocumentNode>,
    val children: List<DocumentNode>,
)

data class DocumentDependency(
    /** Original project-source-relative Snooty file id; origin carries the bundle identity. */
    val requestedPath: String,
    val origin: DocumentOrigin,
    val parserDigest: String?,
)

data class DocumentPage(
    val id: String,
    val raw: DocumentNode?,
    val expanded: DocumentNode?,
    val dependencies: List<DocumentDependency>,
    val dependenciesComplete: Boolean,
)

data class DocumentParser(
    val engine: String,
    val revision: String,
    val version: String,
    val pythonVersion: String,
    val adapterSha256: String,
    val dependencyLockSha256: String,
    val dependencies: Map<String, String>,
)

/** path is bundle-relative, including requested paths for unavailable/generated resources. */
data class DocumentDiagnostic(
    val code: String,
    val path: String,
    val line: Int?,
    val severity: String,
    val message: String,
)

enum class DocumentState {
    READY,
    PARTIAL,
}

data class DocumentReport(
    val schemaVersion: Int = 1,
    val sourceSha256: String,
    val inputSha256: String,
    val parser: DocumentParser,
    val state: DocumentState,
    val pages: List<DocumentPage>,
    val diagnostics: List<DocumentDiagnostic>,
    val unresolvedUrls: List<String>,
) {
    init {
        require(schemaVersion == 1)
        require(pages.isNotEmpty() && pages.map { it.id }.distinct().size == pages.size)
        require(
            (state == DocumentState.READY) ==
                (diagnostics.isEmpty() &&
                    unresolvedUrls.isEmpty() &&
                    pages.all { it.raw != null && it.expanded != null && it.dependenciesComplete })
        ) {
            "Document state contradicts parsed pages/diagnostics"
        }
    }
}
