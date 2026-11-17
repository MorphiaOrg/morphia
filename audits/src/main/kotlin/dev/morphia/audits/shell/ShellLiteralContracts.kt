package dev.morphia.audits.shell

import org.bson.BsonValue

/** Optional parser hints: bounded half-open UTF-16 offsets, excluding known unusable extents. */
data class ShellSpan(val start: Int, val end: Int)

enum class ShellDialect {
    MONGOSH,
    LEGACY_MONGO,
    UNSPECIFIED,
}

enum class ShellLiteralState {
    NORMALIZED,
    UNSUPPORTED,
    INVALID,
}

data class ShellLiteralProblem(val code: String, val message: String, val span: ShellSpan?)

/**
 * Owns a fresh BSON value; callers must not mutate it concurrently with downstream comparison. This
 * in-process result is not a JSON wire format or an execution/coverage attestation.
 */
data class ShellLiteralResult(
    val state: ShellLiteralState,
    val source: String,
    val sourceSha256: String?,
    val parser: String,
    val dialect: ShellDialect,
    val value: BsonValue?,
    val problems: List<ShellLiteralProblem>,
)

data class ShellLiteralLimits(
    val maximumCharacters: Int = 262_144,
    val maximumNodes: Int = 50_000,
    val maximumDepth: Int = 64,
) {
    init {
        require(
            maximumCharacters in 1..1_048_576 &&
                maximumNodes in 1..100_000 &&
                maximumDepth in 1..128
        )
    }
}
