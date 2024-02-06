package dev.morphia.audits.rst

import dev.morphia.audits.RstAuditor
import java.io.File
import java.io.StringWriter
import org.testng.Assert.assertEquals
import org.testng.Assert.assertNull
import org.testng.annotations.Test

class RstDocumentTest {
    @Test
    fun testAbs() {
        val examples = readAggOperator("abs").examples
        assertEquals(examples.size, 1)
        val example = examples.first()
        val data = StringWriter().also { example.dataBlock?.write(it) }
        val action = StringWriter().also { example.actionBlock?.write(it) }
        val expected = StringWriter().also { example.expectedBlock?.write(it) }

        assertEquals(
            data.toString(),
            """
            { _id: 1, startTemp: 50, endTemp: 80 },
            { _id: 2, startTemp: 40, endTemp: 40 },
            { _id: 3, startTemp: 90, endTemp: 70 },
            { _id: 4, startTemp: 60, endTemp: 70 }
        """
                .trimIndent(),
            "Data block should match"
        )
        assertEquals(
            expected.toString(),
            """
               { "_id" : 1, "delta" : 30 }
               { "_id" : 2, "delta" : 0 }
               { "_id" : 3, "delta" : 20 }
               { "_id" : 4, "delta" : 10 }
        """
                .trimIndent(),
            "Expected block should match"
        )
        assertEquals(
            action.toString(),
            """
            [
               {
                  ${"$"}project: { delta: { ${"$"}abs: { ${"$"}subtract: [ "${"$"}startTemp", "${"$"}endTemp" ] } } }
               }
            ]
        """
                .trimIndent(),
            "Action block should match"
        )

        assertNull(example.indexBlock)
    }

    @Test
    fun testAcosh() {
        val examples = readAggOperator("acosh").examples
        assertEquals(examples.size, 2)
        val example = examples.first()
        val data = StringWriter().also { example.dataBlock?.write(it) }
        val action = StringWriter().also { example.actionBlock?.write(it) }
        val expected = StringWriter().also { example.expectedBlock?.write(it) }

        assertEquals(
            data.toString(),
            "{  \"_id\" : ObjectId(\"5c50782193f833234ba90d85\"),  \"x-coordinate\" : NumberDecimal(\"3\")}",
            "Data block should match"
        )
        assertEquals(
            action.toString(),
            """
            [
              { 
                ${"$"}addFields : {
                  "y-coordinate" : {
                    ${"$"}radiansToDegrees : { ${"$"}acosh : "${"$"}x-coordinate" }
                  }
                }
              }
            ]
        """
                .trimIndent(),
            "Action block should match"
        )
        assertEquals(
            expected.toString(),
            "{  \"_id\" : ObjectId(\"5c50782193f833234ba90d85\"),  \"x-coordinate\" : NumberDecimal(\"3\"),  \"y-coordinate\" : " +
                "NumberDecimal(\"100.9979734210524228844295260083432\")}",
            "Expected block should match"
        )

        assertNull(example.indexBlock)
    }

    @Test
    fun testAcos() {
        val examples = readAggOperator("acos").examples

        assertEquals(
            examples.size,
            2,
            "Should have a example0 for each tab: ${examples.map { it.name }}"
        )
    }

    @Test
    fun testAccumulator() {
        val examples = readAggOperator("accumulator").examples
        assertEquals(examples.size, 2)
        assertEquals(
            examples.first().name,
            "Use ``\$accumulator`` to Implement the ``\$avg`` Operator"
        )
    }

    @Test
    fun testMeta() {
        val examples = readAggOperator("meta").examples

        assertEquals(
            examples.size,
            7,
            "Should have a subsection for each tab: ${examples.map { it.name }}"
        )
        validateExample("main", examples[0], ExampleValidator(false))
        var name = "\$meta: \"textScore\""
        val validator = ExampleValidator(index = true)
        validateExample("$name :: Aggregation", examples[1], validator)
        validateExample("$name :: Find and Project", examples[2], validator)

        name = "\$meta: \"indexKey\""
        validateExample("$name :: Aggregation", examples[3], validator)
        validateExample("$name :: Find and Project", examples[4], validator)

        validateExample("$name :: Aggregation [1]", examples[5], validator)
        validateExample("$name :: Find and Project [1]", examples[6], validator)
    }

    private fun validateExample(
        name: String,
        example: OperatorExample,
        validator: ExampleValidator = ExampleValidator(),
    ) {
        validator.name(example, name)
        validator.data(example)
        validator.action(example)
        validator.expected(example)
        validator.index(example)
    }

    private fun readAggOperator(operator: String): RstDocument {
        return RstDocument.read(operator, File("${RstAuditor.aggRoot}/${operator}.txt"))
    }
}
