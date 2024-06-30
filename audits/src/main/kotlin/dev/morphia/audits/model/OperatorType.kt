package dev.morphia.audits.model

enum class OperatorType {
    EXPRESSION {
        override fun docsName() = "aggregation-expressions"

        override fun taglet() = "@aggregation.expression"

        override fun root() = "aggregation"

        override fun path() = "expressions"
    },
    STAGE {
        override fun docsName() = "aggregation-stages"

        override fun taglet() = "@aggregation.stage"

        override fun root() = "aggregation"

        override fun path() = "stages"
    },
    FILTER {
        override fun docsName() = "query-filters"

        override fun taglet() = "@query.filter"

        override fun root() = "query"

        override fun path() = "filters"
    },
    UPDATE {
        override fun docsName() = "update-operators"

        override fun taglet() = "@update.operator"

        override fun docsRoot() = "update"

        override fun root() = "query"

        override fun path() = "updates"
    };

    abstract fun docsName(): String

    abstract fun taglet(): String

    open fun docsRoot() = root()

    abstract fun root(): String

    abstract fun path(): String
}
