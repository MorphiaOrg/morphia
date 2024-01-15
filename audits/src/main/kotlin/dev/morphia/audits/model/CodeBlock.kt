package dev.morphia.audits.model

import dev.morphia.audits.findIndent
import dev.morphia.audits.notControl
import java.io.Writer

class CodeBlock {
    private var prefix = ""
    var indent: Int = 0
        set(value) {
            field = value
            prefix = "%${field}s".format("")
        }

    private val lines = mutableListOf<String>()

    operator fun plusAssign(line: String) {
        if (!line.isBlank() && line.findIndent() >= indent && notControl(line)) {
            lines += line.removePrefix(prefix).substringBefore("//")
        }
    }

    override fun toString() = lines.joinToString("\n")

    fun hasData() = lines.isNotEmpty()

    fun code(): List<String> {
        return lines
    }

    fun sanitizeData(): List<String> {
        if (isAction()) {
            return sanitizeAction()
        }
        var iterator = lines.iterator()
        var sanitized = mutableListOf<String>()
        while (iterator.hasNext()) {
            var line = iterator.next()
            if (line.contains(".insertMany") || line.contains("insertOne")) {
                line = line.substringAfterLast("(")
                if (line.isBlank()) {
                    line = iterator.next()
                }
            }
            while (line.trim() in listOf("[", "]", "(", ")", "])", "] )")) {
                line = if (iterator.hasNext()) iterator.next() else ""
            }
            line = applyReplacements(line)
            if (line.isNotBlank()) {
                sanitized += collect(line, iterator).trim()
            }
        }

        return sanitized
    }

    private fun sanitizeAction(): List<String> {
        var sanitized = mutableListOf(*lines.toTypedArray())
        val first = sanitized.first()
        if (first.contains("[")) {
            sanitized[0] = first.substringAfterLast("(")
        } else {
            sanitized.removeFirst()
        }
        val last = sanitized.removeLast()
        if (last.contains("]")) {
            sanitized += last.substringBefore(")")
        }

        return sanitized
    }

    private fun applyReplacements(line: String): String {
        var final = line
        val replacements =
            listOf(
                "new Date(" to "ISODate(",
                " Long(" to " NumberLong(",
                " Decimal128(" to " NumberDecimal(",
                " Int32(" to " NumberInt("
            )
        replacements.forEach { r -> final = final.replace(r.first, r.second) }
        return final
    }

    private fun collect(line: String, iterator: MutableIterator<String>): String {
        var line1 = line
        while (iterator.hasNext() && line1.count { it == '{' } != line1.count { it == '}' }) {
            line1 += iterator.next()
            /*
                        var newLine = ""
                        while (line1 != "}") {
                            newLine += line1;
                            try {
                            } catch (e: Exception) {
                                throw e
                            }
                        }
            line1 = newLine + line1
            */
        }
        return line1
    }

    fun contains(text: String): Boolean {
        return hasData() && lines.any { it.contains(text) }
    }

    fun startsWith(text: String): Boolean {
        return hasData() && lines.first().startsWith(text)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeBlock) return false

        if (prefix != other.prefix) return false
        if (indent != other.indent) return false
        if (lines != other.lines) return false

        return true
    }

    override fun hashCode(): Int {
        var result = prefix.hashCode()
        result = 31 * result + indent
        result = 31 * result + lines.hashCode()
        return result
    }

    fun isAction(): Boolean = contains(".aggregate(")

    fun isData(): Boolean = contains(".insertOne(") || contains(".insertMany(")

    fun write(output: Writer) {
        output.write(sanitizeData().joinToString("\n"))
    }
}
