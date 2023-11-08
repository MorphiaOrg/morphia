package dev.morphia.model

import dev.morphia.findIndent
import dev.morphia.notControl

class CodeBlock {
    private var prefix = ""
    var indent: Int = 0
        set(value) {
            field = value
            prefix = "%${field}s".format("")
        }

    private val lines = mutableListOf<String>()

    operator fun plusAssign(line: String) {
        if (!line.isBlank() && findIndent(line) >= indent && notControl(line)) {
            lines += line.removePrefix(prefix).substringBefore("//")
        }
    }

    override fun toString() = lines.joinToString("\n")

    fun hasData() = lines.isNotEmpty()

    fun code(): List<String> {
        return lines
    }

    fun sanitizeData(): List<String> {
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
            if (line.isNotBlank()) {
                sanitized += collect(line, iterator).trim()
            }
        }

        return sanitized
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
}
