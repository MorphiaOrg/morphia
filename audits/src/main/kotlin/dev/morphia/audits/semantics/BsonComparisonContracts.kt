package dev.morphia.audits.semantics

import java.math.BigDecimal

/** RFC 6901 pointer. The empty pointer denotes the value being compared (a row for results). */
@JvmInline
value class BsonPath(val pointer: String) {
    init {
        require(pointer.isEmpty() || pointer.startsWith('/')) { "Invalid BSON pointer: $pointer" }
        require(!invalidEscape.containsMatchIn(pointer)) { "Invalid BSON pointer escape: $pointer" }
    }

    internal fun child(segment: String): BsonPath =
        BsonPath(pointer + "/" + segment.replace("~", "~0").replace("/", "~1"))

    companion object {
        private val invalidEscape = Regex("~(?:[^01]|$)")
        val ROOT = BsonPath("")
    }
}

/** Explicit, same-BSON-type tolerance: |a-b| <= absolute + relative * max(|a|, |b|). */
data class NumericTolerance(
    val absolute: BigDecimal = BigDecimal.ZERO,
    val relative: BigDecimal = BigDecimal.ZERO,
) {
    init {
        require(absolute.signum() >= 0 && relative.signum() >= 0) {
            "Numeric tolerances must be nonnegative"
        }
        require(
            absolute.precision() <= 128 &&
                relative.precision() <= 128 &&
                absolute.scale() in -6144..6144 &&
                relative.scale() in -6144..6144
        ) {
            "Numeric tolerances exceed supported precision or scale"
        }
    }
}

/**
 * Defaults preserve every document's key order, every array's order and all BSON types and
 * representations. Rules name exact paths. Result rules are relative to each row, independent of
 * its position in the result set; a path below an unordered array addresses its items by their
 * expected position. Callers must not mutate inputs during comparison.
 */
data class BsonComparisonPolicy(
    val unorderedDocuments: Set<BsonPath> = emptySet(),
    val numericTolerances: Map<BsonPath, NumericTolerance> = emptyMap(),
    /** Arrays compared as multisets: the same items, as often, in any order. */
    val unorderedArrays: Set<BsonPath> = emptySet(),
)

enum class ResultOrder {
    ORDERED,
    UNORDERED,
}

enum class BsonComparisonState {
    EQUAL,
    DIFFERENT,
    INVALID,
}

enum class BsonDifferenceKind {
    TYPE,
    VALUE,
    MISSING_FIELD,
    EXTRA_FIELD,
    KEY_ORDER,
    ARRAY_LENGTH,
    RESULT_COUNT,
    UNMATCHED_RESULT,
    /** An item of an unordered array has no distinct counterpart. */
    UNMATCHED_ITEM,
}

data class BsonDifference(val path: BsonPath, val kind: BsonDifferenceKind, val message: String)

data class BsonComparisonResult(
    val state: BsonComparisonState,
    val difference: BsonDifference? = null,
    val problem: String? = null,
)

/** Limits apply to the entire comparison, including multiset matching and input validation. */
data class BsonComparisonLimits(
    val maximumDepth: Int = 128,
    val maximumWork: Int = 2_000_000,
    val maximumResults: Int = 512,
    val maximumResultPairs: Int = 100_000,
) {
    init {
        require(
            maximumDepth in 1..256 &&
                maximumWork in 1..10_000_000 &&
                maximumResults in 1..1024 &&
                maximumResultPairs in 1..1_000_000
        ) {
            "BSON comparison limits are outside supported bounds"
        }
    }
}
