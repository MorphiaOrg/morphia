package dev.morphia.audits.offline

import dev.morphia.audits.extraction.BlockRole
import dev.morphia.audits.extraction.Documented
import dev.morphia.audits.extraction.DocumentedScenario
import dev.morphia.audits.extraction.SourceBlock
import org.bson.BsonDecimal128
import org.bson.BsonDocument
import org.bson.types.Decimal128
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** How [Documented] reads an example's data blocks and the databases its commands name. */
class DocumentedTest {
    private fun block(role: BlockRole, text: String) =
        SourceBlock(role, text, Documented.sha256(text.toByteArray()))

    private fun documented(vararg blocks: SourceBlock): DocumentedScenario =
        Documented.documented("example", null, blocks.toList())

    @Test
    fun `an insert spaced before its paren is an insert, not a listing`() {
        val documented =
            documented(
                block(
                    BlockRole.DATA,
                    "db.inventory.insertOne (\n   { _id: 2, tags: [ \"a\" ] }\n)",
                ),
                block(BlockRole.ACTION, "db.inventory.find({ _id: 2 })"),
            )
        assertEquals(
            listOf(BsonDocument.parse("{ _id: 2, tags: [ 'a' ] }")),
            documented.data["inventory"],
        )
    }

    @Test
    fun `a listing reads the constructors mongosh prints`() {
        val documented =
            documented(
                block(BlockRole.DATA, "{\n  \"_id\" : 1,\n  \"angle\" : Decimal128(\"53.13\")\n}"),
                block(BlockRole.ACTION, "db.trigonometry.find({})"),
            )
        assertEquals(
            BsonDecimal128(Decimal128.parse("53.13")),
            documented.data.getValue("trigonometry").single()["angle"],
        )
    }

    @Test
    fun `one named database is the one the example runs in`() {
        val documented =
            documented(
                block(
                    BlockRole.DATA,
                    "db.getSiblingDB(\"examples\").sensors.insertMany([ { _id: 1 } ])",
                ),
                block(BlockRole.ACTION, "db.sensors.find({})"),
            )
        assertEquals("sensors", documented.collection)
        assertEquals(listOf(BsonDocument.parse("{ _id: 1 }")), documented.data["sensors"])
    }

    @Test
    fun `several named databases reach outside the scenario's`() {
        val failure =
            assertThrows<IllegalArgumentException> {
                documented(
                    block(
                        BlockRole.DATA,
                        "db.getSiblingDB(\"a\").items.insertMany([ { _id: 1 } ])",
                    ),
                    block(BlockRole.ACTION, "db.getSiblingDB(\"b\").items.find({})"),
                )
            }
        assertTrue("command outside the current database" in failure.message!!, failure.message)
    }
}
