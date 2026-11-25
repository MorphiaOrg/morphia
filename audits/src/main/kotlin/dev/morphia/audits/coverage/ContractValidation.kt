package dev.morphia.audits.coverage

/** Shared checks on the paths and operator tokens the audit contracts carry. */
internal object ContractValidation {
    fun relative(path: String, root: Boolean = false): Boolean =
        (root && path == ".") ||
            (path.isNotBlank() &&
                !path.startsWith('/') &&
                '\\' !in path &&
                ':' !in path &&
                path.split('/').none { it in setOf("", ".", "..") })

    fun operatorToken(value: String): String? = OperatorTokens.leading(value)
}
