package dev.morphia.audits.offline

import dev.morphia.audits.repairs.NumericRetype
import dev.morphia.audits.repairs.NumericRetypes
import dev.morphia.audits.repairs.NumericType
import org.bson.BsonDocument
import org.bson.BsonDouble
import org.bson.BsonInt32
import org.bson.BsonInt64
import org.bson.BsonValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NumericRetypesTest {
    private fun values(json: String): List<BsonValue> =
        BsonDocument.parse("{v: $json}").getArray("v").values

    @Test
    fun conversionsKeepTheExactValue() {
        assertEquals(BsonDouble(3.0), NumericRetypes.convert(BsonInt32(3), NumericType.DOUBLE))
        assertEquals(BsonInt32(3), NumericRetypes.convert(BsonDouble(3.0), NumericType.INT32))
        assertNull(NumericRetypes.convert(BsonDouble(2.5), NumericType.INT32))
        assertNull(NumericRetypes.convert(BsonDouble(-0.0), NumericType.INT32))
        assertNull(NumericRetypes.convert(BsonDouble(Double.NaN), NumericType.INT64))
        assertNull(NumericRetypes.convert(BsonInt64(Long.MAX_VALUE), NumericType.INT32))
        // 2^53 + 1 has no double of the same value.
        assertNull(NumericRetypes.convert(BsonInt64((1L shl 53) + 1), NumericType.DOUBLE))
    }

    @Test
    fun retypesChangeOnlyTheirSubtreeAndType() {
        val input = values("""[{_id: 1, loc: [1, 2], n: {${'$'}numberLong: "3"}}]""")
        val (output, counts) =
            NumericRetypes.apply(
                input,
                listOf(NumericRetype("data.json", "/0/loc", NumericType.INT32, NumericType.DOUBLE)),
            )!!
        assertEquals(values("""[{_id: 1, loc: [1.0, 2.0], n: {${'$'}numberLong: "3"}}]"""), output)
        assertEquals(listOf(2), counts)
        // The input is copied, not modified.
        assertEquals(BsonInt32(1), input[0].asDocument().getArray("loc")[0])
        val whole =
            NumericRetypes.apply(
                input,
                listOf(NumericRetype("data.json", "", NumericType.INT64, NumericType.INT32)),
            )!!
        assertEquals(listOf(1), whole.second)
        assertNull(
            NumericRetypes.apply(
                input,
                listOf(NumericRetype("data.json", "/0/gone", NumericType.INT32, NumericType.DOUBLE)),
            )
        )
        assertNull(
            NumericRetypes.apply(
                values("[{a: 2.5}]"),
                listOf(NumericRetype("data.json", "", NumericType.DOUBLE, NumericType.INT32)),
            )
        )
    }

    @Test
    fun commonPointer() {
        assertEquals("/0/a", NumericRetypes.common(listOf("/0/a/0", "/0/a/1/x", "/0/a")))
        assertEquals("", NumericRetypes.common(listOf("/0/a", "/1/a")))
    }
}
