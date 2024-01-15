package dev.morphia.audits

import org.testng.Assert.assertEquals
import org.testng.annotations.Test

class OperationAuditTest {
    @Test
    fun audits() {
        var remaining = 0
        var created = 0

        with(
            OperationAudit.parse(taglet = "@query.filter")
                .audit(
                    "query-filters",
                    "https://docs.mongodb.com/manual/reference/operator/query/",
                    listOf("$", "\$rand")
                )
        ) {
            remaining += first
            created += second
        }

        with(
            OperationAudit.parse(taglet = "@update.operator")
                .audit(
                    "update-operators",
                    "https://docs.mongodb.com/manual/reference/operator/update/",
                    listOf("$", "$[]", "$[<identifier>]", "\$position", "\$slice", "\$sort")
                )
        ) {
            remaining += first
            created += second
        }

        println("$remaining items to handle")
        assertEquals(created, 0)
    }
}
