package dev.morphia.audits

import dev.morphia.audits.RstAuditor.operatorRoot
import dev.morphia.audits.model.Operator
import dev.morphia.audits.model.OperatorType.EXPRESSION
import dev.morphia.audits.model.OperatorType.STAGE
import java.io.File
import org.testng.annotations.Test

class AggregationAuditTest : BaseAuditTest() {

    @Test
    fun testOperator() {
        val name = "geoNear"
        var operator = Operator(EXPRESSION, File("${operatorRoot}/$name.txt"))
        operator.examples.forEach { it.output(File("target/testOperator-${name}/${it.name}")) }
    }

    @Test
    fun expressions() {
        validate(RstAuditor.audit(EXPRESSION))
    }

    @Test
    fun stages() {
        validate(RstAuditor.audit(STAGE))
    }
}
