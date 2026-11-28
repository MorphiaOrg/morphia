package dev.morphia.audits.shell

import dev.morphia.audits.published.PublishedArray
import dev.morphia.audits.published.PublishedDocument
import dev.morphia.audits.published.PublishedText
import dev.morphia.audits.published.PublishedValue
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.BsonSymbol
import org.bson.BsonValue

/**
 * Finds the ellipses the docs' tested examples use in printed results. Their comparison
 * (`code-example-tests/comparison-spec.md` in mongodb/docs) reads a bare `...` as an omitted value,
 * array items or fields, a `'...'` string as any value, and a string ending in `...` as a truncated
 * one. Results that elide prove only part of what the server returns, so the audit doesn't check
 * them; the scenario's serialization check still holds Morphia to the documented command.
 *
 * Strings are judged as the parser read them, so escapes, regular expressions and constructor
 * arguments read exactly. Only a block the parser rejects is scanned for a bare `...`.
 */
object ShellEllipsis {
    /** Whether printed text elides: it is `...`, or it ends with it. */
    fun elidesText(text: String): Boolean = text.trimEnd().endsWith("...")

    /** Whether a printed result holds eliding text, as a field value or an array item. */
    fun elides(value: PublishedValue): Boolean =
        when (value) {
            is PublishedText -> elidesText(value.value)
            is PublishedDocument -> value.fields.any { elides(it.value) }
            is PublishedArray -> value.items.any { elides(it) }
            else -> false
        }

    /** Whether a result read as BSON holds eliding text, as a field value or an array item. */
    fun elides(value: BsonValue): Boolean =
        when (value) {
            is BsonString -> elidesText(value.value)
            is BsonSymbol -> elidesText(value.symbol)
            is BsonDocument -> value.values.any { elides(it) }
            is BsonArray -> value.values.any { elides(it) }
            else -> false
        }

    private val terminators = setOf('\n', '\r', '\u2028', '\u2029')

    /**
     * Whether a block the parser rejected elides: a `...` outside strings and comments, a string
     * that is `...`, or a result string that ends with it. A constructor's argument is a value, not
     * printed text, so only an exact `'...'` one elides, as in `ObjectId('...')`. The scan is
     * lexical, not a parser: it only decides whether a block that can't be read is an elided one.
     */
    fun scan(source: String): Boolean {
        var i = 0
        // How deep in a call's parentheses the scan is, and the last code character it passed.
        var depth = 0
        var previous = ' '
        while (i < source.length) {
            val c = source[i]
            when {
                source.startsWith("//", i) -> {
                    while (i < source.length && source[i] !in terminators) i++
                }
                source.startsWith("/*", i) ->
                    i = source.indexOf("*/", i + 2).let { if (it < 0) source.length else it + 2 }
                c == '\'' || c == '"' || c == '`' -> {
                    val argument = depth > 0 && (previous == '(' || previous == ',')
                    val (text, end) = string(source, i)
                    i = end
                    val trimmed = text.trim()
                    if (trimmed == "..." || !argument && elidesText(text)) return true
                    previous = c
                }
                source.startsWith("...", i) -> return true
                else -> {
                    if (c == '(') depth++
                    if (c == ')' && depth > 0) depth--
                    if (!c.isWhitespace()) previous = c
                    i++
                }
            }
        }
        return false
    }

    /** The decoded text of the string literal opening at [start], and the index after it. */
    private fun string(source: String, start: Int): Pair<String, Int> {
        val quote = source[start]
        val text = StringBuilder()
        var i = start + 1
        while (i < source.length && source[i] != quote) {
            if (source[i] != '\\' || i + 1 >= source.length) {
                text.append(source[i++])
                continue
            }
            val escape = source[i + 1]
            val hex =
                when {
                    escape == 'u' && source.startsWith("{", i + 2) ->
                        source.substring(i + 3, source.indexOf('}', i + 3).coerceAtLeast(i + 3))
                    escape == 'u' -> source.substring(i + 2, minOf(i + 6, source.length))
                    escape == 'x' -> source.substring(i + 2, minOf(i + 4, source.length))
                    else -> null
                }
            val code = hex?.toIntOrNull(16)
            if (code != null && code in 0..0x10FFFF) {
                text.appendCodePoint(code)
                i += 2 + hex.length + if (source.startsWith("{", i + 2)) 2 else 0
            } else {
                text.append(escape)
                i += 2
            }
        }
        return text.toString() to i + 1
    }
}
