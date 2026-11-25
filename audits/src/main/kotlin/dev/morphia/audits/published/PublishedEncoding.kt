package dev.morphia.audits.published

import dev.morphia.audits.semantics.BsonSnapshot
import org.bson.BsonArray
import org.bson.BsonBoolean
import org.bson.BsonDecimal128
import org.bson.BsonDocument
import org.bson.BsonDouble
import org.bson.BsonInt32
import org.bson.BsonString
import org.bson.BsonSymbol
import org.bson.BsonValue

/**
 * Published constraints as BSON, for `scenario.json` and the golden vectors core's `PrintedResults`
 * reads. Each node is a one-key document naming its kind, so no field name or value of the
 * published text is read as a kind: `{document: [{name, value}]}`, `{array: [..]}`, `{exact: v}`,
 * `{whole: n}`, `{text: s}`, `{nan: true}` and `{decimal: s}`.
 */
object PublishedEncoding {
    fun encode(value: PublishedValue): BsonDocument =
        when (value) {
            is PublishedDocument ->
                BsonDocument(
                    "document",
                    BsonArray(
                        value.fields.map {
                            BsonDocument("name", BsonString(it.name))
                                .append("value", encode(it.value))
                        }
                    ),
                )
            is PublishedArray -> BsonDocument("array", BsonArray(value.items.map { encode(it) }))
            is PublishedExact -> BsonDocument("exact", value.value.value())
            is PublishedWholeNumber -> BsonDocument("whole", BsonInt32(value.value))
            is PublishedText -> BsonDocument("text", BsonString(value.value))
            PublishedDoubleNaN -> BsonDocument("nan", BsonBoolean.TRUE)
            is PublishedDecimal128 -> BsonDocument("decimal", BsonString(value.value))
        }

    /**
     * What printing [value] as the pinned mongosh profile prints it proves: an Int32 or an integral
     * Double in the Int32 range is a whole number, a string or symbol is text, a Decimal128 keeps
     * its text, a Double NaN is NaN, a document lists its fields in ECMAScript own-property order,
     * and every other scalar (a Long, for example, printed `Long("5")`) is exact.
     */
    fun printed(value: BsonValue): PublishedValue =
        when (value) {
            is BsonDocument ->
                PublishedDocument(
                    JavaScriptKeyOrder.order(value.keys.toList()).map {
                        PublishedField(it, printed(value[it]!!))
                    }
                )
            is BsonArray -> PublishedArray(value.values.map { printed(it) })
            is BsonInt32 -> PublishedWholeNumber(value.value)
            is BsonDouble ->
                when {
                    value.value.isNaN() -> PublishedDoubleNaN
                    value.value == Math.rint(value.value) &&
                        value.value >= Int.MIN_VALUE &&
                        value.value <= Int.MAX_VALUE &&
                        !(value.value == 0.0 && 1 / value.value < 0) ->
                        PublishedWholeNumber(value.value.toInt())
                    else -> PublishedExact(BsonSnapshot.of(value))
                }
            is BsonString -> PublishedText(value.value)
            is BsonSymbol -> PublishedText(value.symbol)
            is BsonDecimal128 -> PublishedDecimal128(value.value.toString())
            else -> PublishedExact(BsonSnapshot.of(value))
        }

    fun decode(value: BsonValue): PublishedValue {
        require(value is BsonDocument && value.size == 1) {
            "A published constraint is a one-key document"
        }
        val (kind, argument) = value.entries.single()
        return when (kind) {
            "document" ->
                PublishedDocument(
                    argument.asArray().map {
                        val field = it.asDocument()
                        require(field.keys == setOf("name", "value")) {
                            "A published field is {name, value}"
                        }
                        PublishedField(field.getString("name").value, decode(field["value"]!!))
                    }
                )
            "array" -> PublishedArray(argument.asArray().map { decode(it) })
            "exact" -> PublishedExact(BsonSnapshot.of(argument))
            "whole" -> PublishedWholeNumber(argument.asInt32().value)
            "text" -> PublishedText(argument.asString().value)
            "nan" -> {
                require(argument == BsonBoolean.TRUE) { "nan takes true" }
                PublishedDoubleNaN
            }
            "decimal" -> PublishedDecimal128(argument.asString().value)
            else -> throw IllegalArgumentException("Unknown published constraint $kind")
        }
    }
}
