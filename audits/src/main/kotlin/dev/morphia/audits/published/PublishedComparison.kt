package dev.morphia.audits.published

import dev.morphia.audits.semantics.*
import java.nio.charset.CodingErrorAction
import java.util.IdentityHashMap
import org.bson.*

/**
 * Compares a published read-result constraint with BSON. Typed DSL/reference parity uses
 * BsonComparison.
 */
object PublishedComparison {
    fun compareResults(
        expected: List<PublishedDocument>,
        actual: List<BsonDocument>,
        order: ResultOrder = ResultOrder.ORDERED,
        limits: BsonComparisonLimits = BsonComparisonLimits(),
    ): BsonComparisonResult =
        try {
            val mismatch = Engine(limits).compare(expected, actual, order)
            BsonComparisonResult(
                if (mismatch == null) BsonComparisonState.EQUAL else BsonComparisonState.DIFFERENT,
                mismatch,
            )
        } catch (failure: Invalid) {
            BsonComparisonResult(BsonComparisonState.INVALID, problem = failure.message)
        }

    private class Invalid(message: String) : RuntimeException(message)

    private class Engine(val limits: BsonComparisonLimits) {
        var work = 0L
        val scalars = IdentityHashMap<PublishedExact, BsonValue>()

        fun check(condition: Boolean, message: String) {
            if (!condition) throw Invalid(message)
        }

        fun spend(amount: Int = 1) {
            work += amount.toLong()
            check(work <= limits.maximumWork, "Published comparison work limit exceeded")
        }

        fun text(value: String, key: Boolean = false) {
            spend(value.length)
            check(!key || '\u0000' !in value, "BSON key contains NUL")
            check(
                Charsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .canEncode(value),
                "Unpaired Unicode surrogate in published comparison",
            )
        }

        fun validatePattern(
            value: PublishedValue?,
            depth: Int,
            ancestors: IdentityHashMap<PublishedValue, Boolean>,
        ) {
            spend()
            val node = value ?: throw Invalid("Null published constraint")
            check(depth <= limits.maximumDepth, "Published constraint depth limit exceeded")
            check(ancestors.put(node, true) == null, "Cyclic published constraint")
            when (node) {
                is PublishedDocument -> {
                    val keys = HashSet<String>()
                    node.fields.forEach { field: PublishedField? ->
                        val it = field ?: throw Invalid("Null published field")
                        text(it.name, true)
                        check(keys.add(it.name), "Duplicate published field")
                        validatePattern(it.value, depth + 1, ancestors)
                    }
                    check(
                        !("\$ref" in keys && "\$id" in keys),
                        "DBRef-shaped published documents need an explicit rule",
                    )
                    check(
                        JavaScriptKeyOrder.order(node.fields.map { it.name }) ==
                            node.fields.map { it.name },
                        "Published fields must use ECMAScript own-property order",
                    )
                }
                is PublishedArray ->
                    node.items.forEach { validatePattern(it, depth + 1, ancestors) }
                is PublishedText -> text(node.value)
                is PublishedExact -> {
                    spend(node.value.byteLength)
                    scalars[node] = node.value.value()
                }
                is PublishedDecimal128 -> text(node.value)
                is PublishedWholeNumber,
                PublishedDoubleNaN -> Unit
            }
            ancestors.remove(node)
        }

        fun validateActual(
            value: BsonValue?,
            depth: Int,
            ancestors: IdentityHashMap<BsonValue, Boolean>,
        ) {
            spend()
            val node = value ?: throw Invalid("Null BSON value; use BsonNull")
            check(depth <= limits.maximumDepth, "BSON depth limit exceeded")
            check(ancestors.put(node, true) == null, "Cyclic BSON input")
            when (node.bsonType) {
                BsonType.DOCUMENT -> {
                    check(
                        node.javaClass == BsonDocument::class.java,
                        "Only plain BsonDocument inputs are supported",
                    )
                    node.asDocument().forEach { (key, item) ->
                        text(key, true)
                        validateActual(item, depth + 1, ancestors)
                    }
                }
                BsonType.ARRAY -> {
                    check(
                        node.javaClass == BsonArray::class.java,
                        "Only plain BsonArray inputs are supported",
                    )
                    node.asArray().forEach { validateActual(it, depth + 1, ancestors) }
                }
                BsonType.STRING -> text(node.asString().value)
                BsonType.SYMBOL -> text(node.asSymbol().symbol)
                BsonType.BINARY -> spend(node.asBinary().data.size)
                BsonType.REGULAR_EXPRESSION -> {
                    text(node.asRegularExpression().pattern)
                    text(node.asRegularExpression().options)
                }
                BsonType.JAVASCRIPT -> text(node.asJavaScript().code)
                BsonType.JAVASCRIPT_WITH_SCOPE -> {
                    text(node.asJavaScriptWithScope().code)
                    validateActual(node.asJavaScriptWithScope().scope, depth + 1, ancestors)
                }
                BsonType.DB_POINTER -> text(node.asDBPointer().namespace)
                else -> Unit
            }
            ancestors.remove(node)
        }

        private fun scalarCost(value: BsonValue): Int =
            1 +
                when (value.bsonType) {
                    BsonType.STRING -> value.asString().value.length
                    BsonType.SYMBOL -> value.asSymbol().symbol.length
                    BsonType.BINARY -> value.asBinary().data.size
                    BsonType.REGULAR_EXPRESSION ->
                        value.asRegularExpression().pattern.length +
                            value.asRegularExpression().options.length
                    BsonType.JAVASCRIPT -> value.asJavaScript().code.length
                    BsonType.DB_POINTER -> value.asDBPointer().namespace.length
                    else -> 0
                }

        fun difference(
            expected: PublishedValue,
            actual: BsonValue,
            path: BsonPath,
        ): BsonDifference? {
            spend()
            fun mismatch(kind: BsonDifferenceKind = BsonDifferenceKind.VALUE) =
                BsonDifference(path, kind, "Published ${expected.kind} constraint differs")
            when (expected) {
                is PublishedDocument -> {
                    if (!actual.isDocument) return mismatch(BsonDifferenceKind.TYPE)
                    expected.fields.forEach { spend(it.name.length + 1) }
                    val fields = expected.fields.associateBy { it.name }
                    val document = actual.asDocument()
                    fields.keys
                        .firstOrNull { it !in document }
                        ?.let {
                            return BsonDifference(
                                path.child(it),
                                BsonDifferenceKind.MISSING_FIELD,
                                "Missing field",
                            )
                        }
                    document.keys
                        .firstOrNull { it !in fields }
                        ?.let {
                            return BsonDifference(
                                path.child(it),
                                BsonDifferenceKind.EXTRA_FIELD,
                                "Unexpected field",
                            )
                        }
                    spend(fields.size)
                    if (fields.keys.toList() != JavaScriptKeyOrder.order(document.keys.toList()))
                        return mismatch(BsonDifferenceKind.KEY_ORDER)
                    expected.fields.forEach { field ->
                        difference(
                                field.value,
                                document.getValue(field.name),
                                path.child(field.name),
                            )
                            ?.let {
                                return it
                            }
                    }
                    return null
                }
                is PublishedArray -> {
                    if (!actual.isArray) return mismatch(BsonDifferenceKind.TYPE)
                    if (expected.items.size != actual.asArray().size)
                        return mismatch(BsonDifferenceKind.ARRAY_LENGTH)
                    expected.items.forEachIndexed { index, item ->
                        difference(item, actual.asArray()[index], path.child(index.toString()))
                            ?.let {
                                return it
                            }
                    }
                    return null
                }
                is PublishedWholeNumber -> {
                    val matches =
                        when (actual) {
                            is BsonInt32 -> actual.value == expected.value
                            is BsonDouble ->
                                actual.value.toRawBits() == expected.value.toDouble().toRawBits()
                            else -> false
                        }
                    return if (matches) null else mismatch()
                }
                is PublishedText -> {
                    spend(expected.value.length)
                    val matches =
                        when (actual) {
                            is BsonString -> actual.value == expected.value
                            is BsonSymbol -> actual.symbol == expected.value
                            else -> false
                        }
                    return if (matches) null else mismatch()
                }
                PublishedDoubleNaN ->
                    return if (actual is BsonDouble && actual.value.isNaN()) null else mismatch()
                is PublishedDecimal128 ->
                    return if (
                        actual is BsonDecimal128 && actual.value.toString() == expected.value
                    )
                        null
                    else mismatch()
                is PublishedExact -> {
                    if (expected.value.bsonType != actual.bsonType)
                        return mismatch(BsonDifferenceKind.TYPE)
                    spend(expected.value.byteLength)
                    spend(scalarCost(actual))
                    val result =
                        BsonComparison.compare(scalars.getValue(expected), actual, limits = limits)
                    check(
                        result.state != BsonComparisonState.INVALID,
                        result.problem ?: "Invalid exact scalar",
                    )
                    return result.difference?.copy(path = path)
                }
            }
        }

        fun compare(
            expected: List<PublishedDocument>,
            actual: List<BsonDocument>,
            order: ResultOrder,
        ): BsonDifference? {
            check(
                expected.size <= limits.maximumResults && actual.size <= limits.maximumResults,
                "Result count exceeds comparison limit",
            )
            expected.forEach { validatePattern(it, 0, IdentityHashMap()) }
            actual.forEach { validateActual(it, 0, IdentityHashMap()) }
            if (expected.size != actual.size)
                return BsonDifference(
                    BsonPath.ROOT,
                    BsonDifferenceKind.RESULT_COUNT,
                    "Result counts differ",
                )
            if (order == ResultOrder.ORDERED) {
                expected.indices.forEach { index ->
                    difference(expected[index], actual[index], BsonPath("/$index"))?.let {
                        return it
                    }
                }
                return null
            }
            check(
                expected.size.toLong() * actual.size <= limits.maximumResultPairs,
                "Result pair count exceeds comparison limit",
            )
            val candidates =
                expected.map { left ->
                    actual.map { right -> difference(left, right, BsonPath.ROOT) == null }
                }
            // Constraints overlap: an Int32-or-Double row must not consume the sole match of an
            // exact Double row.
            val owners = IntArray(actual.size) { -1 }
            val assigned = IntArray(expected.size) { -1 }
            for (root in expected.indices) {
                val previous = IntArray(actual.size) { -1 }
                val queue = ArrayDeque<Int>()
                queue.add(root)
                var free = -1
                search@ while (queue.isNotEmpty()) {
                    val left = queue.removeFirst()
                    for (right in actual.indices) {
                        spend()
                        if (!candidates[left][right] || previous[right] != -1) continue
                        previous[right] = left
                        if (owners[right] == -1) {
                            free = right
                            break@search
                        }
                        queue.add(owners[right])
                    }
                }
                if (free == -1)
                    return BsonDifference(
                        BsonPath("/$root"),
                        BsonDifferenceKind.UNMATCHED_RESULT,
                        "No multiplicity-preserving assignment",
                    )
                while (free != -1) {
                    spend()
                    val left = previous[free]
                    val old = assigned[left]
                    owners[free] = left
                    assigned[left] = free
                    free = old
                }
            }
            return null
        }
    }
}
