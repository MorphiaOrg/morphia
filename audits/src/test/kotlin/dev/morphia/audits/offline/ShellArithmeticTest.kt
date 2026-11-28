package dev.morphia.audits.offline

import dev.morphia.audits.shell.*
import org.bson.BsonArray
import org.bson.BsonDateTime
import org.bson.BsonDocument
import org.bson.BsonDouble
import org.bson.BsonInt32
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShellArithmeticTest {
    private fun normalize(vararg blocks: ProgramBlock) =
        ShellPrograms.normalize(
            ShellProgramRequest(dialect = ShellDialect.MONGOSH, blocks = blocks.toList())
        )

    private fun pipeline(action: String): BsonArray {
        val result = normalize(ProgramBlock("action", action, ProgramRole.ACTION))
        assertEquals(ShellLiteralState.NORMALIZED, result.state, "$action: ${result.problems}")
        val operation = result.commands.single().operation as AggregateOperation
        return operation.pipeline.value().asArray()
    }

    /** The value of `{ $project: { v: <expression> } }`. */
    private fun projected(expression: String) =
        pipeline("db.sales.aggregate([ { \$project: { v: $expression } } ])")[0]
            .asDocument()
            .getDocument("\$project")["v"]

    @Test
    fun constantArithmeticIsTheNumberTheShellSends() {
        assertEquals(BsonInt32(259_200_000), projected("3*24*60*60000"))
        assertEquals(BsonInt32(300_000), projected("5 * 60 * 1000"))
        assertEquals(BsonDouble(10 / 3963.2), projected("10/3963.2"))
        assertEquals(BsonInt32(-7), projected("-(3 + 4)"))
        assertEquals(BsonInt32(1), projected("7 % 3"))
        assertEquals(BsonInt32(-1), projected("-7 % 3"))
        assertEquals(BsonDouble(2.5), projected("5 / 2"))
        // Beyond int32, as a literal of the same value would be.
        assertEquals(BsonDouble(16.0 * 1024 * 1024 * 1024), projected("16 * 1024 * 1024 * 1024"))
    }

    @Test
    fun arithmeticOverAnythingButNumbersIsUnsupported() {
        for (expression in
            listOf(
                "'a' + 'b'",
                "1 + '2'",
                "x * 2",
                "Math.PI * 2",
                "'a'.repeat(3) + 1",
                "true + 1",
            )) {
            val result =
                normalize(
                    ProgramBlock(
                        "action",
                        "db.sales.aggregate([ { \$project: { v: $expression } } ])",
                        ProgramRole.ACTION,
                    )
                )
            assertNotEquals(ShellLiteralState.NORMALIZED, result.state, expression)
        }
    }

    @Test
    fun printedResultsNeverCompute() {
        // However deep the arithmetic sits, a printed result is what the server returned.
        for (results in
            listOf(
                "[ { v: 1+2 } ]",
                "[ { v: -(-(1+2)) } ]",
                "[ { v: NumberInt(1+2) } ]",
                "[ { v: Double(0/0) } ]",
            )) {
            val result =
                normalize(
                    ProgramBlock("action", "db.items.find({});", ProgramRole.ACTION),
                    ProgramBlock(
                        "result",
                        results,
                        ProgramRole.EXPECTED,
                        ProgramFormat.SHELL_RESULTS,
                    ),
                )
            assertNotEquals(ShellLiteralState.NORMALIZED, result.state, results)
        }
        // The same constructors read arithmetic in the page's data, as the shell computes it.
        assertEquals(BsonInt32(3), projected("NumberInt(1+2)"))
    }

    @Test
    fun aBindingAPrintedResultReachesIsPrintedToo() {
        val action = "const n = 1+2;\ndb.items.find({ v: n });"
        // The page's command computes with it...
        val command = normalize(ProgramBlock("action", action, ProgramRole.ACTION))
        assertEquals(ShellLiteralState.NORMALIZED, command.state, command.problems.toString())
        assertEquals(
            BsonInt32(3),
            (command.commands.single().operation as FindOperation).filter.value().asDocument()["v"],
        )
        // ...but no printed result does, however it reaches the binding.
        for (results in
            listOf("[ { v: n } ]", "[ { v: -n } ]", "[ { v: +n } ]", "[ { v: NumberInt(n) } ]")) {
            val result =
                normalize(
                    ProgramBlock("action", action, ProgramRole.ACTION),
                    ProgramBlock(
                        "result",
                        results,
                        ProgramRole.EXPECTED,
                        ProgramFormat.SHELL_RESULTS,
                    ),
                )
            assertNotEquals(ShellLiteralState.NORMALIZED, result.state, results)
        }
    }

    @Test
    fun theAddPagesExampleReadsWithItsIsoDates() {
        val result =
            normalize(
                ProgramBlock(
                    "data",
                    """
                    db.sales.insertMany( [
                       { _id : 1, "item" : "abc", "price" : 10, "fee" : 2, date: ISODate("2014-03-01T08:00:00Z") },
                       { _id : 2, "item" : "jkl", "price" : 20, "fee" : 1, date: ISODate("2014-03-01T09:00:00Z") }
                    ] )
                    """
                        .trimIndent(),
                    ProgramRole.SETUP,
                ),
                ProgramBlock(
                    "action",
                    "db.sales.aggregate( [ { \$project: { item: 1, billing_date: { \$add: [ \"\$date\", 3*24*60*60000 ] } } } ] )",
                    ProgramRole.ACTION,
                ),
                ProgramBlock(
                    "result",
                    """
                    { "_id" : 1, "item" : "abc", "billing_date" : ISODate("2014-03-04T08:00:00Z") }
                    { "_id" : 2, "item" : "jkl", "billing_date" : ISODate("2014-03-04T09:00:00Z") }
                    """
                        .trimIndent(),
                    ProgramRole.EXPECTED,
                    ProgramFormat.LEGACY_DOCUMENTS,
                ),
            )
        assertEquals(ShellLiteralState.NORMALIZED, result.state, result.problems.toString())
        val insert = result.commands.first().operation as InsertOperation
        assertEquals(
            BsonDateTime(1_393_660_800_000),
            insert.documents.first().value().asDocument()["date"],
        )
        assertEquals(1, result.expectations.size)
        assertFalse(result.expectations.single().elided)
    }

    @Test
    fun isoDateAndNewDateReadAlike() {
        val data = { date: String ->
            normalize(
                ProgramBlock("data", "db.c.insertOne({ d: $date })", ProgramRole.SETUP),
                ProgramBlock("action", "db.c.find({})", ProgramRole.ACTION),
            )
        }
        fun date(source: String): BsonDocument {
            val result = data(source)
            assertEquals(ShellLiteralState.NORMALIZED, result.state, "$source: ${result.problems}")
            return (result.commands.first().operation as InsertOperation)
                .documents
                .single()
                .value()
                .asDocument()
        }
        assertEquals(
            date("new Date(\"2014-03-01T08:00:00Z\")"),
            date("ISODate(\"2014-03-01T08:00:00Z\")"),
        )
        // Without an offset, ISODate reads UTC; new Date would read the machine's zone, so only
        // ISODate is readable.
        assertEquals(
            date("ISODate(\"2014-03-01T08:00:00Z\")"),
            date("ISODate(\"2014-03-01T08:00:00\")"),
        )
        assertNotEquals(
            ShellLiteralState.NORMALIZED,
            data("new Date(\"2014-03-01T08:00:00\")").state,
        )
    }
}
