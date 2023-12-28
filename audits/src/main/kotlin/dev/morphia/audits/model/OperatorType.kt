package dev.morphia.audits.model

enum class OperatorType {
    EXPRESSION {
        override fun docsName() = "aggregation-expressions"
    },
    STAGE {
        override fun docsName() = "aggregation-stages"
    };

    abstract fun docsName(): String
}
