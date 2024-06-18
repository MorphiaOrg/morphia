package dev.morphia.audits

import dev.morphia.audits.model.Operator
import dev.morphia.audits.model.OperatorType.FILTER
import java.io.File
import org.testng.annotations.Test

class QueryAuditTest : BaseAuditTest() {
    @Test
    fun filters() {
        validate(RstAuditor(FILTER).audit())
    }

    @Test
    fun testOperator() {
        val name = "ne"
        val operator = Operator(FILTER, name)
        operator.examples.forEach { it.output(File("target/testOperator-${name}/${it.name}")) }
    }
}
