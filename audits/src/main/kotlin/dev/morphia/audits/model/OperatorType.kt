package dev.morphia.audits.model

enum class OperatorType {
    EXPRESSION {
        override fun docsName() = "aggregation-expressions"

        override fun taglet() = "@aggregation.expression"

        override fun root() = "aggregation"
    },
    FILTER {
        override fun docsName() = "query-filters"

        override fun taglet() = "@query.filter"

        override fun root() = "query"
    },
    STAGE {
        override fun docsName() = "aggregation-stages"

        override fun taglet() = "@aggregation.stage"

        override fun root() = "aggregation"
    };

    abstract fun docsName(): String

    abstract fun taglet(): String

    abstract fun root(): String
}
