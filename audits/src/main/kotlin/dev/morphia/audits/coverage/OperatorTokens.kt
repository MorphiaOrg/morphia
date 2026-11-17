package dev.morphia.audits.coverage

/** Logical operator identities, including MongoDB's three positional update/projection forms. */
object OperatorTokens {
    private val named = Regex("\\$[A-Za-z_][A-Za-z0-9_]*")
    private val positional = setOf("$", "$[]", "$[<identifier>]")

    fun valid(value: String): Boolean = named.matches(value) || value in positional

    fun leading(value: String): String? =
        value.trim().split(Regex("\\s+"), limit = 2).first().takeIf(::valid)
}
