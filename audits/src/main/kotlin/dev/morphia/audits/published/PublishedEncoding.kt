package dev.morphia.audits.published

import dev.morphia.audits.semantics.BsonSnapshot
import org.bson.BsonArray
import org.bson.BsonBoolean
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString
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
