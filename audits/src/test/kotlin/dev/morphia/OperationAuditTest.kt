package dev.morphia

import org.testng.annotations.Test

class OperationAuditTest {
    @Test
    fun audits() {
        var remaining = 0

        remaining += OperationAudit
            .parse(taglet = "@query.filter")
            .audit(
                "query-filters", "https://docs.mongodb.com/manual/reference/operator/query/",
                listOf("$", "\$rand")
            )

        remaining += OperationAudit
            .parse(taglet = "@update.operator")
            .audit(
                "update-operators", "https://docs.mongodb.com/manual/reference/operator/update/",
                listOf("$", "$[]", "$[<identifier>]", "\$position", "\$slice", "\$sort")
            )

        remaining += OperationAudit
            .parse(taglet = "@aggregation.expression")
            .audit(
                "aggregation-pipeline", "https://docs.mongodb.com/manual/reference/operator/aggregation-pipeline",
                listOf("$", "\$listSessions", "\$listLocalSessions", "\$search" /* not terribly well doc'd.  atlas only? */)
            )

        remaining += OperationAudit
            .parse(taglet = "@aggregation.expression")
            .audit(
                "aggregation-expressions", "https://docs.mongodb.com/manual/reference/operator/aggregation/index.html",
                listOf("$", "\$addFields", "\$group", "\$project", "\$set")
            )

        println("$remaining items to handle")
    }
}