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

    @Test
    fun `a find's sort, skip and limit are its options, in the order the cursor applies them`() {
        val documented =
            documented(
                block(BlockRole.DATA, "db.items.insertMany([ { a: 1 } ])"),
                block(
                    BlockRole.ACTION,
                    "db.items.find({ a: 1 }, { a: 1 }).limit(2).sort({ a: -1 }).skip(1)",
                ),
            )
        assertEquals(
            BsonDocument.parse("{ sort: { a: -1 }, skip: 1, limit: 2 }"),
            documented.options,
        )
        assertEquals(
            listOf(BsonDocument.parse("{ a: 1 }"), BsonDocument.parse("{ a: 1 }")),
            documented.action,
        )
    }

    @Test
    fun `a find modifier the scenario can't carry is a gap`() {
        val failure =
            assertThrows<IllegalArgumentException> {
                documented(
                    block(BlockRole.DATA, "db.items.insertMany([ { a: 1 } ])"),
                    block(BlockRole.ACTION, "db.items.find({ a: 1 }).hint({ a: 1 })"),
                )
            }
        assertTrue("a find with HINT" in failure.message!!, failure.message)
    }

    @Test
    fun `a skip or limit of 0 is no option`() {
        val documented =
            documented(
                block(BlockRole.DATA, "db.items.insertMany([ { a: 1 } ])"),
                block(BlockRole.ACTION, "db.items.find({ a: 1 }).skip(0).limit(0)"),
            )
        assertEquals(null, documented.options)
    }

    @Test
    fun `an update's arrayFilters and upsert are its options, and an upsert of false is none`() {
        val data = block(BlockRole.DATA, "db.items.insertMany([ { _id: 1, a: [ 1 ] } ])")
        val filtered =
            documented(
                data,
                block(
                    BlockRole.ACTION,
                    "db.items.updateMany({}, { \$set: { \"a.\$[e]\": 2 } }, { upsert: false, arrayFilters: [ { e: 1 } ] })",
                ),
            )
        assertEquals(BsonDocument.parse("{ arrayFilters: [ { e: 1 } ] }"), filtered.options)
        val upserted =
            documented(
                data,
                block(
                    BlockRole.ACTION,
                    "db.items.updateOne({ _id: 2 }, { \$set: { a: 2 } }, { upsert: true })",
                ),
            )
        assertEquals(BsonDocument.parse("{ upsert: true }"), upserted.options)
    }

    @Test
    fun `an update option the scenario can't carry is a gap`() {
        val failure =
            assertThrows<IllegalArgumentException> {
                documented(
                    block(BlockRole.DATA, "db.items.insertMany([ { _id: 1 } ])"),
                    block(
                        BlockRole.ACTION,
                        "db.items.updateOne({}, { \$set: { a: 1 } }, { hint: { _id: 1 } })",
                    ),
                )
            }
        assertTrue("an update with hint" in failure.message!!, failure.message)
    }
}
