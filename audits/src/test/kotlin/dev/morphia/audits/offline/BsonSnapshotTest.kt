package dev.morphia.audits.offline

import dev.morphia.audits.semantics.*
import org.bson.*
import org.bson.types.Decimal128
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BsonSnapshotTest {
    @Test
    fun retainsTypesKeyOrderNestedArraysAndReservedEjsonKeys() {
        val values =
            listOf<BsonValue>(
                BsonInt32(1),
                BsonInt64(1),
                BsonDouble(-0.0),
                BsonDecimal128(Decimal128.parse("1.00")),
                BsonNull.VALUE,
                BsonUndefined(),
                BsonDocument("z", BsonInt32(1))
                    .append("a", BsonArray(listOf(BsonInt64(1), BsonInt32(1)))),
                BsonDocument("\$numberLong", BsonString("1")),
                BsonRegularExpression("a/b", "im"),
                BsonBinary(128.toByte(), byteArrayOf(0, 1, -1)),
            )
        for (value in values) {
            val snapshot = BsonSnapshot.of(value)
            assertEquals(value.bsonType, snapshot.bsonType)
            assertEquals(
                BsonComparisonState.EQUAL,
                BsonComparison.compare(value, snapshot.value()).state,
            )
            assertEquals(snapshot, BsonSnapshot.of(snapshot.value()))
        }
        assertNotEquals(BsonSnapshot.of(BsonInt32(1)), BsonSnapshot.of(BsonInt64(1)))
        assertNotEquals(
            BsonSnapshot.of(BsonDocument.parse("{a:1,b:2}")),
            BsonSnapshot.of(BsonDocument.parse("{b:2,a:1}")),
        )
    }

    @Test
    fun snapshotsOwnTheirDataAndDecodeFreshTrees() {
        val input = BsonDocument("a", BsonInt32(1))
        val snapshot = BsonSnapshot.of(input)
        input["a"] = BsonInt32(2)
        snapshot.value().asDocument()["a"] = BsonInt32(3)
        assertEquals(1, snapshot.value().asDocument()["a"]!!.asInt32().value)
    }

    @Test
    fun serializationLimitsAndInvalidValuesNeverProduceSnapshots() {
        assertThrows(BsonSnapshotLimit::class.java) {
            BsonSnapshot.of(BsonString("x".repeat(100)), 32)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BsonSnapshot.of(RawBsonDocument.parse("{a:1}"))
        }
        val cycle = BsonArray()
        cycle.add(cycle)
        assertThrows(IllegalArgumentException::class.java) { BsonSnapshot.of(cycle) }
        assertThrows(IllegalArgumentException::class.java) {
            BsonSnapshot.of(BsonString("x" + '\uD800'))
        }
    }
}
