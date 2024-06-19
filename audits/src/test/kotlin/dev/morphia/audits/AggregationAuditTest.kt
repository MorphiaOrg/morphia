package dev.morphia.audits

import dev.morphia.audits.model.Operator
import dev.morphia.audits.model.OperatorType.EXPRESSION
import dev.morphia.audits.model.OperatorType.STAGE
import java.io.File
import org.testng.annotations.Test

class AggregationAuditTest : BaseAuditTest() {

    @Test
    fun testOperator() {
        val name = "sinh"
        val operator = Operator(EXPRESSION, name)
        operator.examples.forEach { it.output(File("target/testOperator-${name}/${it.name}")) }
    }

    @Test
    fun expressions() {
        validate(RstAuditor(EXPRESSION).audit())
    }

    @Test
    fun stages() {
        validate(RstAuditor(STAGE).audit())
    }
}
