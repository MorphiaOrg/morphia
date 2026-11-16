package dev.morphia.audits.sources

import dev.morphia.audits.coverage.SourceChannel
import dev.morphia.audits.coverage.SourceRole

/** Explicit immutable source selection; no branch names, worktree discovery or implicit fetch. */
data class GitSourceRequest(
    val schemaVersion: Int = 1,
    val id: String,
    val uri: String,
    val revision: String,
    val projectRoot: String,
    val label: String,
    val role: SourceRole,
    val channel: SourceChannel,
    val roots: List<String>,
    val allowedSymlinkRoots: List<String>,
    val maximumFiles: Int,
    val maximumFileBytes: Long,
    val maximumTotalBytes: Long,
    val timeoutSeconds: Long,
)

enum class GitResourceKind {
    FILE,
    EXECUTABLE,
    SYMLINK,
}

data class GitResource(
    val path: String,
    val kind: GitResourceKind,
    val gitObject: String,
    val sha256: String,
    val size: Long,
    val linkTarget: String?,
)

data class SourceProblem(
    val code: String,
    val path: String,
    val chain: List<String>,
    val message: String,
)

enum class SourceState {
    READY,
    PARTIAL,
}

/** Content hash covers the selected raw objects and pins, excluding the reporting tool version. */
data class GitSourceManifest(
    val schemaVersion: Int = 1,
    val request: GitSourceRequest,
    val tree: String,
    val gitVersion: String,
    val contentSha256: String,
    val state: SourceState,
    val resources: List<GitResource>,
    val problems: List<SourceProblem>,
)

enum class AcquisitionState {
    READY,
    PARTIAL,
    INVALID,
}

data class SourceAcquisitionReport(
    val schemaVersion: Int = 1,
    val state: AcquisitionState,
    val manifest: GitSourceManifest?,
    val problems: List<SourceProblem>,
) {
    init {
        require(schemaVersion == 1)
        require(
            if (state == AcquisitionState.INVALID) manifest == null && problems.isNotEmpty()
            else
                manifest != null &&
                    problems == manifest.problems &&
                    ((state == AcquisitionState.READY) == (manifest.state == SourceState.READY))
        ) {
            "Acquisition state contradicts its manifest/diagnostics"
        }
    }
}
