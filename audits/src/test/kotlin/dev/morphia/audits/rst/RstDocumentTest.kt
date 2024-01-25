package dev.morphia.audits.rst

import dev.morphia.audits.RstAuditor
import dev.morphia.audits.model.CodeBlock
import java.io.File
import java.io.StringWriter
import org.testng.Assert.assertEquals
import org.testng.Assert.assertNotNull
import org.testng.Assert.assertNotSame
import org.testng.Assert.assertNull
import org.testng.Assert.assertSame
import org.testng.annotations.Test

class RstDocumentTest {
    @Test
    fun testAbs() {
        val document = readAggOperator("abs")
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
        val document = readAggOperator("acos")
        val subsections = document.examples().subsections()

        assertEquals(subsections.size, 2, "Should have a subsection for each tab")
        validateTabs(
            firstName = "main - degrees tab",
            subsections[0],
            secondName = "main - radians tab",
            subsections[1],
            sameSections = SameSections()
        )
    }

    @Test
    fun testMeta() {
        var document = readAggOperator("meta")
        val subsections = document.examples().subsections()

        assertEquals(subsections.size, 7, "Should have a subsection for each tab")
        validateSection(subsections[0], "main", HasSections(false))
        var name = "``\$meta: \"textScore\"``"
        validateTabs(
            firstName = "$name - Aggregation tab",
            subsections[1],
            secondName = "$name - Find and Project tab",
            subsections[2],
            HasSections(index = true),
            SameSections(data = true, index = true)
        )
        name = "``\$meta: \"indexKey\"``"
        validateTabs(
            firstName = "$name - Aggregation tab",
            subsections[3],
            secondName = "$name - Find and Project tab",
            subsections[4],
            HasSections(index = true),
            SameSections(data = true, expected = true, index = true)
        )
        validateTabs(
            firstName = "$name - Aggregation tab",
            subsections[5],
            secondName = "$name - Find and Project tab",
            subsections[6],
            HasSections(index = true),
            SameSections(data = true, expected = true, index = true)
        )
    }

    private fun validateTabs(
        firstName: String,
        first: Section,
        secondName: String,
        second: Section,
        hasSections: HasSections = HasSections(),
        sameSections: SameSections
    ) {
        validateSection(first, firstName, hasSections)
        validateSection(second, secondName, hasSections)

        isSame("data", sameSections.data, first.dataBlock, second.dataBlock)
        isSame("action", sameSections.action, first.actionBlock, second.actionBlock)
        isSame("expected", sameSections.expected, first.expectedBlock, second.expectedBlock)
        if (hasSections.index) {
            isSame("index", sameSections.index, first.indexBlock, second.indexBlock)
        }
    }

    private fun isSame(blockName: String, same: Boolean, first: CodeBlock?, second: CodeBlock?) {
        if (same) {
            assertSame(first, second, "Should have the same $blockName block")
        } else {
            assertNotSame(first, second, "Should have unique $blockName blocks")
        }
    }

    private fun validateSection(
        section: Section,
        name: String,
        hasSections: HasSections = HasSections()
    ) {
        assertEquals(section.name, name, "Should have the correct name")
        if (hasSections.data) {
            assertNotNull(section.dataBlock, "Should have a data block")
        } else {
            assertNull(section.dataBlock, "Should not have a data block")
        }

        if (hasSections.action) {
            assertNotNull(section.actionBlock, "Should have a action block")
        } else {
            assertNull(section.actionBlock, "Should not have a action block")
        }

        if (hasSections.expected) {
            assertNotNull(section.expectedBlock, "Should have an expected block")
        } else {
            assertNull(section.expectedBlock, "Should not have an expected block")
        }

        if (hasSections.index) {
            assertNotNull(section.indexBlock, "Should have an index block")
        } else {
            assertNull(section.indexBlock, "Should not have an index block")
        }
    }

    private fun readAggOperator(operator: String) =
        RstDocument.read(File("${RstAuditor.aggRoot}/${operator}.txt"))
}

data class HasSections(
    val data: Boolean = true,
    val action: Boolean = true,
    val expected: Boolean = true,
    val index: Boolean = false
) {
    constructor(global: Boolean) : this(global, global, global, global)
}

data class SameSections(
    val data: Boolean = false,
    val action: Boolean = false,
    val expected: Boolean = false,
    val index: Boolean = false
) {
    constructor(global: Boolean) : this(global, global, global, global)
}
