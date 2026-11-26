package dev.morphia.audits.offline

import com.fasterxml.jackson.databind.ObjectMapper
import dev.morphia.audits.extraction.BlockRole
import dev.morphia.audits.extraction.ExampleOverride
import dev.morphia.audits.extraction.ExampleOverrides
import dev.morphia.audits.extraction.Overrides
import dev.morphia.audits.extraction.ScenarioPatch
import dev.morphia.audits.extraction.ScenarioWriter
import dev.morphia.audits.extraction.SourceBlock
import org.bson.BsonDocument
import org.bson.BsonInt64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OverridesTest {
    private val json = ObjectMapper()

    private fun override(vararg patches: ScenarioPatch) =
        ExampleOverride(
            "aggregation/expressions/abs",
            "main",
            "0".repeat(64),
            "reason",
            patches = patches.toList(),
        )

    @Test
    fun aPatchReplacesTheValueItWasReviewedAgainst() {
        val scenario = BsonDocument.parse("{collections: {items: [{_id: 1, qty: 5}]}}")
        val from =
            Overrides.digest(
                scenario.getDocument("collections").getArray("items")[0].asDocument()["qty"]!!
            )
        Overrides.patch(
            scenario,
            override(
                ScenarioPatch(
                    "/collections/items/0/qty",
                    from,
                    json.readTree("""{"${'$'}numberLong": "5"}"""),
                )
            ),
        )
        assertEquals(
            BsonInt64(5),
            scenario.getDocument("collections").getArray("items")[0].asDocument()["qty"],
        )
    }

    @Test
    fun aPatchFailsOnceThePageChanges() {
        val scenario = BsonDocument.parse("{collections: {items: [{_id: 1, qty: 6}]}}")
        val reviewed = Overrides.digest(BsonDocument.parse("{v: 5}")["v"]!!)
        val failure =
            assertThrows<IllegalArgumentException> {
                Overrides.patch(
                    scenario,
                    override(
                        ScenarioPatch("/collections/items/0/qty", reviewed, json.readTree("7"))
                    ),
                )
            }
        assertTrue(
            failure.message!!.contains("has changed since the patch was reviewed"),
            failure.message,
        )
    }

    @Test
    fun aPatchToNoValueFails() {
        val failure =
            assertThrows<IllegalArgumentException> {
                Overrides.patch(
                    BsonDocument(),
                    override(ScenarioPatch("/action/pipeline/3", "x", json.readTree("1"))),
                )
            }
        assertTrue(failure.message!!.contains("names no value"), failure.message)
    }

    @Test
    fun theIdentityFollowsTheBlocks() {
        val action = SourceBlock(BlockRole.ACTION, "db.c.find()", "a".repeat(64))
        val data = SourceBlock(BlockRole.DATA, "db.c.insertOne({})", "b".repeat(64))
        assertEquals(
            Overrides.identity(listOf(data, action)),
            Overrides.identity(listOf(data, action)),
        )
        assertNotEquals(
            Overrides.identity(listOf(data, action)),
            Overrides.identity(listOf(action, data)),
        )
    }

    @Test
    fun anExclusionChangesNothingElse() {
        val excluded =
            ExampleOverride(
                "aggregation/stages/out",
                "main",
                "0".repeat(64),
                "reason",
                exclude = true,
                order = "ORDERED",
            )
        assertEquals(
            listOf(
                "aggregation/stages/out \"main\" is excluded, so it can't also change its scenario"
            ),
            Overrides.problems(ExampleOverrides(overrides = listOf(excluded))),
        )
    }

    @Test
    fun aBehaviorSkipSkipsPublished() {
        val skip =
            ExampleOverride(
                "query/filters/eq",
                "main",
                "0".repeat(64),
                "reason",
                skipped = mapOf("behavior" to "why"),
            )
        assertEquals(
            listOf("query/filters/eq \"main\" skips behavior, so it must skip published too"),
            Overrides.problems(ExampleOverrides(overrides = listOf(skip))),
        )
    }

    @Test
    fun aPageNameBecomesItsTestsFolder() {
        assertEquals("maxNArrayElement", ScenarioWriter.folder("maxN-array-element"))
        assertEquals("positionalAll", ScenarioWriter.folder("positional-all"))
        assertEquals("abs", ScenarioWriter.folder("abs"))
    }
}
