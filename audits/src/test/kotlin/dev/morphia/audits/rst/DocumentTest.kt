package dev.morphia.audits.rst

import dev.morphia.audits.RstAuditor
import dev.morphia.audits.model.OperatorType.EXPRESSION
import dev.morphia.audits.model.OperatorType.FILTER
import java.io.File
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

class DocumentTest {
    @Test
    fun testNeFilter() {
        val auditor = RstAuditor(FILTER)

        val source = File(auditor.operatorRoot, "ne.txt")
        val document = Document(source)

        val examples = document.exampleSections()
        assertEquals(3, examples.size)
    }

    @Test
    fun testSinhExpression() {
        val auditor = RstAuditor(EXPRESSION)

        val source = File(auditor.operatorRoot, "sinh.txt")
        val document = Document(source)

        val examples = document.exampleSections()
        assertEquals(2, examples.size)
        assertEquals(examples[0].name, "main :: Hyperbolic Sine of Value in Degrees")
        assertEquals(examples[1].name, "main :: Hyperbolic Sine of Value in Radians")
    }
}
