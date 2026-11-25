package dev.morphia.audits.coverage

import java.util.Locale

enum class OperatorContext {
    QUERY_PREDICATE,
    QUERY_PROJECTION,
    UPDATE_OPERATOR,
    UPDATE_MODIFIER,
    PIPELINE_STAGE,
    AGGREGATION_EXPRESSION,
    GROUP_ACCUMULATOR,
    WINDOW_FUNCTION;

    fun key(): String = name.lowercase(Locale.ROOT).replace('_', '-')
}

data class CapabilityKey(val product: String, val context: OperatorContext, val token: String) {
    fun id(): String = "$product/${context.key()}/$token"
}

data class CapabilityRef(val key: CapabilityKey, val facet: String?) {
    fun id(): String = key.id() + (facet?.let { "/facet/$it" } ?: "")
}

enum class SourceRole {
    CURRENT,
    HISTORICAL,
}

enum class SourceChannel {
    RELEASED,
    UPCOMING,
    UNKNOWN,
}
