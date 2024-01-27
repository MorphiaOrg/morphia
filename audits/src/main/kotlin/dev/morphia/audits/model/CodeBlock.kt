package dev.morphia.audits.model

import dev.morphia.audits.findIndent
import dev.morphia.audits.model.CodeBlock.Type.ACTION
import dev.morphia.audits.model.CodeBlock.Type.DATA
import dev.morphia.audits.model.CodeBlock.Type.EXPECTED
import dev.morphia.audits.model.CodeBlock.Type.INDEX
import dev.morphia.audits.notControl
import dev.morphia.audits.rst.removeWhile
import java.io.Writer

class CodeBlock {
    companion object {
        fun findBlocks(lines: List<String>): Map<Type, MutableList<CodeBlock>> {
            val blocks = mutableListOf<CodeBlock>()
            val data = lines.toMutableList()
            var line: String
            while (data.isNotEmpty()) {
                line = data.removeFirst().trim()
                if (line.trim().startsWith(".. code-block:: ")) {
                    blocks += readBlock(data)
                }
            }
            val grouped =
                blocks
                    .groupBy { it.type }
                    .map { it.key to it.value.toMutableList() }
                    .toMap()
                    .toMutableMap()
            val expected = grouped[EXPECTED]
            val action = blocks.indexOfFirst { it.type == ACTION }
            if (grouped[DATA] == null && expected != null && expected.size > 1 && action != 0) {
                grouped[DATA] = mutableListOf(expected.removeFirst())
            }

            return grouped
        }

        fun readBlock(lines: MutableList<String>): CodeBlock {
            lines.removeWhile { !notControl(it) || it.isBlank() }.toMutableList()
            val block = CodeBlock()
            block.indent = lines.first().findIndent()
            while (
                lines.isNotEmpty() &&
                    (lines.first().findIndent() >= block.indent || lines.first().isBlank())
            ) {
                block += lines.removeFirst()
            }
            return block
        }
    }

    enum class Type {
        DATA,
        ACTION,
        EXPECTED,
        INDEX
    }

    val type: Type by lazy {
        when {
            isData() -> DATA
            isAction() -> ACTION
            isIndex() -> INDEX
            else -> EXPECTED
        }
    }

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

    override fun toString() = "CodeBlock[$type, ${lines.joinToString("\n")}]"

    fun hasData() = lines.isNotEmpty()

    fun code(): List<String> {
        return lines
    }

    fun sanitizeData(): List<String> {
        if (isAction()) {
            return sanitizeAction()
        }
        val iterator = lines.iterator()
        val sanitized = mutableListOf<String>()
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
        val sanitized = mutableListOf(*lines.toTypedArray())
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
        var collected = line
        while (
            iterator.hasNext() && collected.count { it == '{' } != collected.count { it == '}' }
        ) collected += iterator.next()
        return collected
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

    fun isAction(): Boolean = contains(".aggregate(") || contains(".find(")

    fun isData(): Boolean = contains(".insertOne(") || contains(".insertMany(")

    fun isIndex(): Boolean = contains(".createIndex(")

    fun write(output: Writer) {
        output.write(sanitizeData().joinToString("\n"))
    }
}
