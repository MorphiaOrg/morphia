package dev.morphia.audits.shell

import org.bson.BsonRegularExpression
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextAction
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.RhinoException
import org.mozilla.javascript.regexp.RegExpImpl

/** Validate JS regex syntax with the pinned regex compiler; never match or execute a program. */
internal object ShellRegularExpressions {
    fun javascript(
        pattern: String,
        flags: String,
        span: ShellSpan?,
        fromString: Boolean = false,
    ): BsonRegularExpression {
        fun unsupported(code: String, message: String): Nothing =
            throw ShellLiterals.UnsupportedLiteral(code, message, span)
        if (flags.any { it !in "im" } || flags.toSet().size != flags.length)
            unsupported(
                "REGEX_FLAGS",
                "Only i and m are supported for JavaScript regex BSON serialization",
            )
        if (
            pattern.any { it in "\r\n\u2028\u2029" } ||
                fromString && (pattern.isEmpty() || '/' in pattern)
        )
            unsupported(
                "REGEX_SOURCE_FORM",
                "Regex source escaping requires an explicit representation rule",
            )
        if ('\u0000' in pattern)
            unsupported("REGEX_ENCODING", "BSON regex patterns cannot contain NUL")
        if (pattern.length > 4096)
            unsupported("REGEX_LIMIT", "Regex exceeds validation length limit")
        // Resource guard only. Syntax/meaning comes from the maintained regex compiler.
        var escaped = false
        var characterClass = false
        var depth = 0
        for (character in pattern) {
            if (escaped) {
                escaped = false
                continue
            }
            if (character == '\\') {
                escaped = true
                continue
            }
            if (character == '[') characterClass = true
            else if (character == ']') characterClass = false
            else if (!characterClass && character == '(' && ++depth > 64)
                unsupported("REGEX_LIMIT", "Regex exceeds validation nesting limit")
            else if (!characterClass && character == ')') depth--
        }
        try {
            ContextFactory()
                .call(
                    ContextAction { context ->
                        context.languageVersion = Context.VERSION_ECMASCRIPT
                        RegExpImpl().compileRegExp(context, pattern, flags)
                    }
                )
        } catch (failure: RhinoException) {
            unsupported("REGEX_SYNTAX", "Invalid JavaScript regex syntax: ${failure.details()}")
        }
        return BsonRegularExpression(pattern, flags)
    }
}
