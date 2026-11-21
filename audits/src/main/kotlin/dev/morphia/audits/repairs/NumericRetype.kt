package dev.morphia.audits.repairs

import java.math.BigDecimal
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonDouble
import org.bson.BsonInt32
import org.bson.BsonInt64
import org.bson.BsonValue

/** The BSON number types a retype converts between. Decimal128 is not supported. */
enum class NumericType {
    INT32,
    INT64,
    DOUBLE,
}

/**
 * Declared change of BSON number type: every [from] number under [pointer] (RFC 6901) in [file]
 * becomes a [to] number of exactly the same value. The pointer addresses the documented side as it
 * is compared: for `action.json`, the canonical form under the declaration's rules.
 */
data class NumericRetype(
    val file: String,
    val pointer: String,
    val from: NumericType,
    val to: NumericType,
) {
    init {
        require(from != to) { "A retype must change the type: $this" }
        require(pointer.isEmpty() || pointer.startsWith('/')) { "Invalid pointer: $pointer" }
    }
}

object NumericRetypes {
    fun type(value: BsonValue?): NumericType? =
        when (value) {
            is BsonInt32 -> NumericType.INT32
            is BsonInt64 -> NumericType.INT64
            is BsonDouble -> NumericType.DOUBLE
            else -> null
        }

    /** The exact value of a supported number; doubles that are not finite have none. */
    fun exact(value: BsonValue): BigDecimal? =
        when (value) {
            is BsonInt32 -> BigDecimal(value.value)
            is BsonInt64 -> BigDecimal(value.value)
            is BsonDouble -> value.value.takeIf { it.isFinite() }?.let { BigDecimal(it) }
            else -> null
        }

    /** [value] as a [to] number of exactly the same value, or null if there is none. */
    fun convert(value: BsonValue, to: NumericType): BsonValue? {
        val number = exact(value) ?: return null
        val converted =
            when (to) {
                NumericType.DOUBLE -> BsonDouble(number.toDouble())
                NumericType.INT64 ->
                    runCatching { BsonInt64(number.longValueExact()) }.getOrNull() ?: return null
                NumericType.INT32 ->
                    runCatching { BsonInt32(number.intValueExact()) }.getOrNull() ?: return null
            }
        // -0.0 has no integer counterpart that keeps its sign.
        if (value is BsonDouble && value.value == 0.0 && 1 / value.value < 0) return null
        return converted.takeIf { exact(it)!!.compareTo(number) == 0 }
    }

    /**
     * Applies [retypes] (all for one file) to a copy of [values]; returns the copy and how many
     * numbers each retype changed, or null if a pointer does not resolve or a number has no exact
     * counterpart.
     */
    fun apply(
        values: List<BsonValue>,
        retypes: List<NumericRetype>,
    ): Pair<List<BsonValue>, List<Int>>? {
        val root = BsonDocument("v", BsonArray(values)).clone().getArray("v")
        val counts =
            retypes.map { retype ->
                val target = resolve(root, retype.pointer) ?: return null
                var count = 0
                fun visit(value: BsonValue, replace: (BsonValue) -> Unit) {
                    when {
                        type(value) == retype.from -> {
                            replace(convert(value, retype.to) ?: throw Inexact)
                            count++
                        }
                        value is BsonDocument ->
                            value.keys.toList().forEach { key ->
                                visit(value[key]!!) { value[key] = it }
                            }
                        value is BsonArray ->
                            value.indices.forEach { index ->
                                visit(value[index]) { value[index] = it }
                            }
                    }
                }
                try {
                    visit(target.first) { target.second(it) }
                } catch (_: Inexact) {
                    return null
                }
                count
            }
        return root.values to counts
    }

    /** The value at [pointer] in [values], if there is one. */
    fun at(values: List<BsonValue>, pointer: String): BsonValue? =
        resolve(BsonArray(values), pointer)?.first

    /** The value at [pointer] and a way to replace it. */
    private fun resolve(root: BsonArray, pointer: String): Pair<BsonValue, (BsonValue) -> Unit>? {
        if (pointer.isEmpty()) return root to { _ -> error("The root cannot be replaced") }
        var parent: BsonValue = root
        val segments =
            pointer.substring(1).split('/').map { it.replace("~1", "/").replace("~0", "~") }
        segments.dropLast(1).forEach { segment -> parent = child(parent, segment) ?: return null }
        val last = segments.last()
        val value = child(parent, last) ?: return null
        val owner = parent
        return value to
            { replacement ->
                when (owner) {
                    is BsonDocument -> owner[last] = replacement
                    is BsonArray -> owner[last.toInt()] = replacement
                    else -> error("Not a container")
                }
            }
    }

    private fun child(value: BsonValue, segment: String): BsonValue? =
        when (value) {
            is BsonDocument -> value[segment]
            is BsonArray -> segment.toIntOrNull()?.let { value.values.getOrNull(it) }
            else -> null
        }

    /** The deepest pointer that contains every one of [pointers]. */
    fun common(pointers: List<String>): String {
        val split = pointers.map { it.split('/').drop(1) }
        val shared =
            split.first().indices.takeWhile { i -> split.all { it.getOrNull(i) == split[0][i] } }
        return shared.joinToString("") { "/" + split[0][it] }
    }

    private object Inexact : RuntimeException() {
        private fun readResolve(): Any = Inexact
    }
}
