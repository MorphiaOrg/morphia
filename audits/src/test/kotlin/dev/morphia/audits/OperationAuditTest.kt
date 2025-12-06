package dev.morphia.audits

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
                    "https://www.mongodb.com/docs/manual/reference/mql/expressions/",
                    listOf("$", "\$rand"),
                )
        ) {
            remaining += first
            created += second
        }

        with(
            OperationAudit.parse(taglet = "@update.operator")
                .audit(
                    "update-operators",
                    "https://www.mongodb.com/docs/manual/reference/mql/expressions/",
                    listOf("$", "$[]", "$[<identifier>]", "\$position", "\$slice", "\$sort"),
                )
        ) {
            remaining += first
            created += second
        }

        println("$remaining items to handle")
        //        assertEquals(created, 0)
    }
}
