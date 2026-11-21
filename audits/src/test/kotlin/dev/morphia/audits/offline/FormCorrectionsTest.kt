package dev.morphia.audits.offline

import dev.morphia.audits.repairs.FormCorrection
import dev.morphia.audits.repairs.FormCorrections
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString
import org.bson.BsonValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FormCorrectionsTest {
    private fun values(json: String): List<BsonValue> =
        BsonDocument.parse("{v: $json}").getArray("v").values

    @Test
    fun replacesOnlyThePinnedDocumentedValue() {
        val input = values("""[{_id: 1, name: "teaset.jpg"}]""")
        val correction =
            FormCorrection.of(
                "expected.json",
                "/0/name",
                BsonString("teaset.jpg"),
                BsonString("tea_set.jpg"),
            )
        assertEquals(
            FormCorrections.Result.Applied(values("""[{_id: 1, name: "tea_set.jpg"}]""")),
            FormCorrections.apply(input, listOf(correction)),
        )
        // The input is copied, not modified.
        assertEquals(BsonString("teaset.jpg"), input[0].asDocument()["name"])
        // A page that now documents something else makes the correction stale.
        assertEquals(
            FormCorrections.Result.Stale("/0/name"),
            FormCorrections.apply(values("""[{_id: 1, name: "set.jpg"}]"""), listOf(correction)),
        )
        // The pin is BSON-typed: 1 is not 1.0.
        assertEquals(
            FormCorrections.Result.Stale("/0/_id"),
            FormCorrections.apply(
                values("[{_id: 1.0}]"),
                listOf(FormCorrection.of("data.json", "/0/_id", BsonInt32(1), BsonInt32(2))),
            ),
        )
        assertEquals(
            FormCorrections.Result.Stale("/0/gone"),
            FormCorrections.apply(
                input,
                listOf(FormCorrection.of("data.json", "/0/gone", BsonInt32(1), BsonInt32(2))),
            ),
        )
    }

    @Test
    fun fieldsAreRemovedAndAddedInPlace() {
        val input = values("""[{_id: 1, nmae: "g", mobile: "1"}]""")
        val renamed =
            FormCorrections.apply(
                input,
                listOf(
                    FormCorrection.of("data.json", "/0/nmae", BsonString("g"), null),
                    FormCorrection.of("data.json", "/0/name", null, BsonString("g"), 1),
                ),
            )
        assertEquals(
            FormCorrections.Result.Applied(values("""[{_id: 1, name: "g", mobile: "1"}]""")),
            renamed,
        )
        // A position past the end of the document is stale, not appended.
        assertEquals(
            FormCorrections.Result.Stale("/0/x"),
            FormCorrections.apply(
                input,
                listOf(FormCorrection.of("data.json", "/0/x", null, BsonString("2"), 4)),
            ),
        )
        // An added field must not already be documented.
        assertEquals(
            FormCorrections.Result.Stale("/0/mobile"),
            FormCorrections.apply(
                input,
                listOf(FormCorrection.of("data.json", "/0/mobile", null, BsonString("2"), 0)),
            ),
        )
    }

    @Test
    fun aWholeFileOrRowIsNeverReplaced() {
        listOf("", "/0").forEach {
            assertThrows(IllegalArgumentException::class.java) {
                FormCorrection.of("data.json", it, BsonInt32(1), BsonInt32(2))
            }
        }
    }
}
