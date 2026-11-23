package dev.morphia.audits.semantics

import java.math.BigDecimal
import java.util.IdentityHashMap
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonType
import org.bson.BsonValue
import org.bson.types.Decimal128

/** Pure, bounded BSON-tree comparison. No JSON conversion, server access, or snippet execution. */
object BsonComparison {
    fun compare(
        expected: BsonValue?,
        actual: BsonValue?,
        policy: BsonComparisonPolicy = BsonComparisonPolicy(),
        limits: BsonComparisonLimits = BsonComparisonLimits(),
    ): BsonComparisonResult =
        run(policy, limits) {
            validate(expected)
            validate(actual)
            difference(expected!!, actual!!, BsonPath.ROOT)
        }

    /**
     * Unordered means a top-level multiset of rows; nested arrays stay ordered unless the policy
     * names them.
     */
    fun compareResults(
        expected: List<BsonDocument>,
        actual: List<BsonDocument>,
        order: ResultOrder = ResultOrder.ORDERED,
        policy: BsonComparisonPolicy = BsonComparisonPolicy(),
        limits: BsonComparisonLimits = BsonComparisonLimits(),
    ): BsonComparisonResult =
        run(policy, limits) {
            check(expected.size <= limits.maximumResults && actual.size <= limits.maximumResults) {
                "Result count exceeds comparison limit"
            }
            expected.forEach { validate(it) }
            actual.forEach { validate(it) }
            if (expected.size != actual.size) {
                return@run BsonDifference(
                    BsonPath.ROOT,
                    BsonDifferenceKind.RESULT_COUNT,
                    "Expected ${expected.size} results, observed ${actual.size}",
                )
            }
            if (order == ResultOrder.ORDERED) {
                expected.indices.forEach { index ->
                    val mismatch = difference(expected[index], actual[index], BsonPath.ROOT)
                    if (mismatch != null)
                        return@run mismatch.copy(path = BsonPath("/$index" + mismatch.path.pointer))
                }
                return@run null
            }
            check(expected.size.toLong() * actual.size <= limits.maximumResultPairs) {
                "Result pair count exceeds comparison limit"
            }
            // Tolerance makes equivalence non-transitive, so this is a matching, not a removal.
            Matching.firstUnmatched(expected.size, actual.size, ::spend) { left, right ->
                    difference(expected[left], actual[right], BsonPath.ROOT) == null
                }
                ?.let {
                    BsonDifference(
                        BsonPath("/$it"),
                        BsonDifferenceKind.UNMATCHED_RESULT,
                        "Multiplicity-preserving matching fails at expected row $it",
                    )
                }
        }

    private fun run(
        policy: BsonComparisonPolicy,
        limits: BsonComparisonLimits,
        action: Engine.() -> BsonDifference?,
    ): BsonComparisonResult =
        try {
            val engine =
                Engine(
                    policy.copy(
                        unorderedDocuments = policy.unorderedDocuments.toSet(),
                        numericTolerances = policy.numericTolerances.toMap(),
                        unorderedArrays = policy.unorderedArrays.toSet(),
                    ),
                    limits,
                )
            val mismatch = engine.action()
            BsonComparisonResult(
                if (mismatch == null) BsonComparisonState.EQUAL else BsonComparisonState.DIFFERENT,
                mismatch,
            )
        } catch (failure: InvalidComparison) {
            BsonComparisonResult(BsonComparisonState.INVALID, problem = failure.message)
        }

    private class InvalidComparison(message: String) : RuntimeException(message)

    private class Engine(val policy: BsonComparisonPolicy, val limits: BsonComparisonLimits) {
        private var work = 0

        fun spend() {
            check(++work <= limits.maximumWork) { "BSON comparison work limit exceeded" }
        }

        fun check(condition: Boolean, message: () -> String) {
            if (!condition) throw InvalidComparison(message())
        }

        /** Validate all input before comparing, including parts hidden by an early mismatch. */
        fun validate(value: BsonValue?) {
            val ancestors = IdentityHashMap<BsonValue, Boolean>()
            fun visit(value: BsonValue?, depth: Int) {
                spend()
                val node =
                    value ?: throw InvalidComparison("Null BSON value; use BsonNull for BSON null")
                check(depth <= limits.maximumDepth) { "BSON comparison depth limit exceeded" }
                check(ancestors.put(node, true) == null) { "Cyclic BSON input" }
                when (node.bsonType) {
                    BsonType.DOCUMENT -> {
                        check(node.javaClass == BsonDocument::class.java) {
                            "Only plain BsonDocument inputs are supported; raw and lazy containers require validated lowering"
                        }
                        node.asDocument().values.forEach { visit(it, depth + 1) }
                    }
                    BsonType.ARRAY -> {
                        check(node.javaClass == BsonArray::class.java) {
                            "Only plain BsonArray inputs are supported"
                        }
                        node.asArray().values.forEach { visit(it, depth + 1) }
                    }
                    BsonType.JAVASCRIPT_WITH_SCOPE ->
                        visit(node.asJavaScriptWithScope().scope, depth + 1)
                    else -> Unit
                }
                ancestors.remove(node)
            }
            visit(value, 0)
        }

        fun difference(left: BsonValue, right: BsonValue, path: BsonPath): BsonDifference? {
            spend()
            fun mismatch(kind: BsonDifferenceKind, message: String) =
                BsonDifference(path, kind, message)
            if (left.bsonType != right.bsonType)
                return mismatch(
                    BsonDifferenceKind.TYPE,
                    "Expected ${left.bsonType}, observed ${right.bsonType}",
                )
            when (left.bsonType) {
                BsonType.DOCUMENT -> {
                    val expected = left.asDocument()
                    val actual = right.asDocument()
                    expected.keys
                        .firstOrNull { it !in actual }
                        ?.let {
                            return BsonDifference(
                                path.child(it),
                                BsonDifferenceKind.MISSING_FIELD,
                                "Missing field",
                            )
                        }
                    actual.keys
                        .firstOrNull { it !in expected }
                        ?.let {
                            return BsonDifference(
                                path.child(it),
                                BsonDifferenceKind.EXTRA_FIELD,
                                "Unexpected field",
                            )
                        }
                    if (
                        path !in policy.unorderedDocuments &&
                            expected.keys.toList() != actual.keys.toList()
                    ) {
                        return mismatch(BsonDifferenceKind.KEY_ORDER, "Document key order differs")
                    }
                    for (key in expected.keys) {
                        difference(expected.getValue(key), actual.getValue(key), path.child(key))
                            ?.let {
                                return it
                            }
                    }
                    return null
                }
                BsonType.ARRAY -> {
                    val expected = left.asArray()
                    val actual = right.asArray()
                    if (expected.size != actual.size)
                        return mismatch(
                            BsonDifferenceKind.ARRAY_LENGTH,
                            "Expected ${expected.size} array items, observed ${actual.size}",
                        )
                    if (path in policy.unorderedArrays) {
                        check(expected.size.toLong() * actual.size <= limits.maximumResultPairs) {
                            "Unordered array pair count exceeds comparison limit"
                        }
                        return Matching.firstUnmatched(expected.size, actual.size, ::spend) {
                                left,
                                right ->
                                difference(expected[left], actual[right], path.child("$left")) ==
                                    null
                            }
                            ?.let {
                                BsonDifference(
                                    path.child("$it"),
                                    BsonDifferenceKind.UNMATCHED_ITEM,
                                    "No distinct item of the unordered array matches item $it",
                                )
                            }
                    }
                    expected.indices.forEach {
                        difference(expected[it], actual[it], path.child(it.toString()))?.let {
                            return it
                        }
                    }
                    return null
                }
                BsonType.JAVASCRIPT_WITH_SCOPE -> {
                    if (left.asJavaScriptWithScope().code != right.asJavaScriptWithScope().code) {
                        return mismatch(BsonDifferenceKind.VALUE, "JavaScript source differs")
                    }
                    return difference(
                        left.asJavaScriptWithScope().scope,
                        right.asJavaScriptWithScope().scope,
                        path.child("\$scope"),
                    )
                }
                else -> Unit
            }
            val exact =
                when (left.bsonType) {
                    BsonType.DOUBLE ->
                        left.asDouble().value.toRawBits() == right.asDouble().value.toRawBits()
                    BsonType.DECIMAL128 -> {
                        val a = left.asDecimal128().value
                        val b = right.asDecimal128().value
                        a.high == b.high && a.low == b.low
                    }
                    else -> left == right
                }
            if (exact) return null
            val tolerance = policy.numericTolerances[path]
            if (tolerance != null) {
                val a = number(left)
                val b = number(right)
                if (
                    a != null &&
                        b != null &&
                        a.subtract(b).abs() <=
                            tolerance.absolute.add(
                                tolerance.relative.multiply(a.abs().max(b.abs()))
                            )
                )
                    return null
            }
            return mismatch(BsonDifferenceKind.VALUE, "BSON ${left.bsonType} value differs")
        }

        private fun number(value: BsonValue): BigDecimal? =
            when (value.bsonType) {
                BsonType.INT32 -> BigDecimal.valueOf(value.asInt32().value.toLong())
                BsonType.INT64 -> BigDecimal.valueOf(value.asInt64().value)
                BsonType.DOUBLE ->
                    value.asDouble().value.takeIf { it.isFinite() }?.let { BigDecimal.valueOf(it) }
                BsonType.DECIMAL128 ->
                    value
                        .asDecimal128()
                        .value
                        .takeIf { it.isFinite }
                        ?.let {
                            if (it.isNegative)
                                Decimal128.fromIEEE754BIDEncoding(
                                        it.high and Long.MAX_VALUE,
                                        it.low,
                                    )
                                    .bigDecimalValue()
                                    .negate()
                            else it.bigDecimalValue()
                        }
                else -> null
            }
    }
}
