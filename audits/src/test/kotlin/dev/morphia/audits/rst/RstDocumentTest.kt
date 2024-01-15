package dev.morphia.audits.rst

import dev.morphia.audits.RstAuditor
import java.io.File
import java.io.StringWriter
import org.testng.Assert.assertEquals
import org.testng.Assert.assertNotNull
import org.testng.annotations.Test

class RstDocumentTest {
    @Test
    fun testAbs() {
        var document = readAggOperator("abs")
        assertEquals(document.examples().subsections().size, 1)
        val subsection = document.examples().subsections().first()

        val data = StringWriter().also { subsection.dataBlock?.write(it) }
        val action = StringWriter().also { subsection.actionBlock?.write(it) }
        val expected = StringWriter().also { subsection.expectedBlock?.write(it) }

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
    }

    @Test
    fun testAcos() {
        var document = readAggOperator("acos")
        val subsections = document.examples().subsections()

        assertEquals(2, subsections.size, "Should have a subsection for each tab")
        val tab = subsections.first()
        assertNotNull(tab.dataBlock, "Should have a data block")
        assertNotNull(tab.actionBlock, "Should have a action block")
        assertNotNull(tab.expectedBlock, "Should have a expected block")
    }

    @Test
    fun testMeta() {
        var document = readAggOperator("meta")
        val subsections = document.examples().subsections()

        val tab = subsections.first()
        assertNotNull(tab.dataBlock, "Should have a data block")
        assertNotNull(tab.actionBlock, "Should have a action block")
        assertNotNull(tab.expectedBlock, "Should have a expected block")
        assertEquals(subsections.size, 6, "Should have a subsection for each tab")
    }

    private fun readAggOperator(operator: String) =
        RstDocument.read(File("${RstAuditor.aggRoot}/${operator}.txt"))
}
