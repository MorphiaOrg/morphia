package dev.morphia.audits.model

import dev.morphia.audits.findIndent
import dev.morphia.audits.model.CodeBlock.Type.ACTION
import dev.morphia.audits.model.CodeBlock.Type.DATA
import dev.morphia.audits.model.CodeBlock.Type.EXPECTED
import dev.morphia.audits.model.CodeBlock.Type.INDEX
import dev.morphia.audits.notControl
import dev.morphia.audits.rst.removeWhile
import java.io.File
import java.io.Writer

class CodeBlock {
    companion object {
        fun findBlocks(lines: List<String>): MutableList<CodeBlock> {
            val blocks = mutableListOf<CodeBlock>()
            val data = lines.toMutableList()
            while (data.isNotEmpty()) {
                data.removeWhile {
                    !it.trim().startsWith(".. code-block::") &&
                        !it.trim().startsWith(".. io-code-block::")
                }
                if (data.isNotEmpty()) {
                    if (data[0].trim().startsWith(".. code-block::")) {
                        blocks += readBlock(data)
                    } else {
                        blocks += readIoCodeBlock(data)
                    }
                }
            }
            return blocks
        }

        fun readBlock(lines: MutableList<String>): CodeBlock {
            lines.removeWhile { !notControl(it) || it.isBlank() }
            val block = CodeBlock()
            block.indent = lines.first().findIndent()
            while (
                lines.isNotEmpty() &&
                    notControl(lines.first()) &&
                    (lines.first().findIndent() >= block.indent || lines.first().isBlank())
            ) {
                block += lines.removeFirst()
            }
            return block
        }

        private fun readIoCodeBlock(lines: MutableList<String>): List<CodeBlock> {
            lines.removeFirst()
            val indent = lines.first().findIndent()
            lines.removeWhile { it.trim().startsWith(":") || it.isBlank() }
            val blocks = mutableListOf<CodeBlock>()
            var block: CodeBlock? = null
            while (
                lines.isNotEmpty() &&
                    (lines.first().findIndent() >= indent || lines.first().isBlank())
            ) {
                if (lines.first().trim() == ".. input::" || lines.first().trim() == ".. output::") {
                    lines.removeFirst()
                    block =
                        CodeBlock().also { newBlock ->
                            blocks += newBlock
                            newBlock.indent = indent
                        }
                } else {
                    val first = lines.removeFirst()
                    val notControl = notControl(first)
                    if (notControl) {
                        block?.let { it += first }
                    }
                }
            }
            return blocks
        }
    }

    private fun label(index: Int, action: Int) {
        type =
            when {
                isData() -> DATA
                isAction() -> ACTION
                isIndex() -> INDEX
                else -> {
                    if (index < action) DATA else EXPECTED
                }
            }
    }

    enum class Type {
        DATA,
        ACTION,
        EXPECTED,
        INDEX
    }

    lateinit var type: Type
        private set

    var supplemental: Int? = null

    private var prefix = ""
    var indent: Int = 0
        set(value) {
            field = value
            prefix = "%${field}s".format("")
        }

    val lines = mutableListOf<String>()

    operator fun plusAssign(line: String) {
        if (!line.isBlank() && line.findIndent() >= indent && notControl(line)) {
            lines += line.removePrefix(prefix).substringBefore("//")
        }
        label(0, 0)
    }

    override fun toString() = "CodeBlock[$type, ${lines.joinToString("\n")}]"

    fun hasData() = lines.isNotEmpty()

    fun code(): List<String> {
        return lines
    }

    fun output(output: File, applyReplacements: Boolean = true) {
        if (supplemental != null) {
            File(
                    output.parentFile,
                    output.nameWithoutExtension + supplemental + "." + output.extension
                )
                .writeText(sanitizeData(applyReplacements))
        } else {
            output.writeText(sanitizeData(applyReplacements))
        }
    }

    fun sanitizeData(applyReplacements: Boolean): String {
        if (isAction()) {
            return sanitizeAction(applyReplacements)
        }
        val iterator = lines.iterator()
        val sanitized = mutableListOf<String>()
        while (iterator.hasNext()) {
            var line = iterator.next()
            if (
                line.contains(".insertMany") ||
                    line.contains("insertOne") ||
                    line.contains("createIndex")
            ) {
                line = line.substringAfterLast("(")
                if (line.isBlank()) {
                    line = iterator.next()
                }
                if (line.endsWith(")")) {
                    line = line.dropLast(1)
                } else if (line.endsWith(");")) {
                    line = line.dropLast(2)
                }
            }
            while (line.trim() in listOf("[", "]", "(", ")", "])", "] )")) {
                line = if (iterator.hasNext()) iterator.next() else ""
            }
            line = applyReplacements(line)
            if (line.isNotBlank()) {
                var collected = collect(line, iterator).trim()
                collected = collected.replace(Regex("  +"), " ")
                sanitized += collected
            }
        }

        return sanitized.joinToString("\n")
    }

    private fun sanitizeAction(applyReplacements: Boolean): String {
        var sanitized =
            lines
                .map { line -> if (applyReplacements) applyReplacements(line) else line }
                .map { line -> line.replace(Regex("NumberDecimal\\(\"(.+)\"\\)"), "$1") }
                .filter { it.isNotBlank() }
                .joinToString("\n")

        if (sanitized.endsWith(";")) sanitized = sanitized.dropLast(1).trim()
        if (sanitized.contains(".aggregate"))
            sanitized = sanitized.substringAfter(".aggregate").trim()
        if (sanitized.startsWith("(")) {
            sanitized = sanitized.drop(1).trim()
            if (sanitized.endsWith(")")) sanitized = sanitized.dropLast(1).trim()
        }
        if (sanitized.startsWith("[")) sanitized = sanitized.drop(1).dropLast(1).trim()

        return sanitized
    }

    private fun applyReplacements(line: String): String {
        var final = line
        val replacements =
            listOf(
                "new Date(" to "ISODate(",
                " Long(" to " NumberLong(",
                " Decimal128(" to " NumberDecimal(",
                " Int32(" to " NumberInt(",
                " \$substr: " to " \$substrBytes: "
            )
        replacements.forEach { r -> final = final.replace(r.first, r.second) }
        if (final.contains("/*") && final.contains("*/"))
            final = final.removeRange(final.indexOf("/*")..(final.indexOf("*/") + 1))

        return final
    }

    private fun collect(line: String, iterator: MutableIterator<String>): String {
        var collected = line
        while (
            iterator.hasNext() && collected.count { it == '{' } != collected.count { it == '}' }
        ) collected += applyReplacements(iterator.next())
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

    fun isAction(): Boolean =
        contains(".aggregate(") ||
            contains(".find(") ||
            contains(".updateOne(") ||
            contains(".updateMany(")

    fun isData(): Boolean = contains(".insertOne(") || contains(".insertMany(")

    fun isExpected(): Boolean = !isData() && !isAction() && !isIndex()

    fun isIndex(): Boolean = contains(".createIndex(")

    fun write(output: Writer) {
        output.write(sanitizeData(true))
    }

    fun isPipeline(): Boolean {
        return isAction() && lines.any { it.contains(".aggregate(") }
    }
}
