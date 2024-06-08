package dev.morphia.audits

import dev.morphia.audits.model.OperatorType.FILTER
import org.testng.annotations.Test

class QueryAuditTest : BaseAuditTest() {
    @Test
    fun filters() {
        validate(RstAuditor.audit(FILTER))
    }
}
