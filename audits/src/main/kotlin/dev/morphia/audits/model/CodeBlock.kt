package dev.morphia.audits.model

import dev.morphia.audits.rst.findIndent

/**
 * Extracts a limited set of inline RST blocks without interpreting or rewriting their code.
 * External snippets require the future pinned source resolver and fail explicitly here.
 */
class CodeBlock private constructor(private val lines: List<String>) {
    companion object {
        fun findBlocks(lines: List<String>): MutableList<CodeBlock> {
            val remaining = lines.toMutableList()
            val blocks = mutableListOf<CodeBlock>()
            while (remaining.isNotEmpty()) {
                val line = remaining.first().trim()
                when {
                    line.startsWith(".. code-block::") -> blocks += readBlock(remaining)
                    line.startsWith(".. io-code-block::") -> blocks += readIoBlock(remaining)
                    line.startsWith(".. include::") ||
                        line.startsWith(".. literalinclude::") ||
                        line.startsWith(".. sharedinclude::") ->
                        throw UnsupportedOperationException(
                            "External snippet requires a source resolver: $line"
                        )
                    else -> remaining.removeFirst()
                }
            }
            return blocks
        }

        private fun readBlock(lines: MutableList<String>): CodeBlock {
            val directive = lines.removeFirst()
            val directiveIndent = directive.findIndent()
            while (
                lines.isNotEmpty() &&
                    lines.first().isNotBlank() &&
                    lines.first().findIndent() > directiveIndent &&
                    lines.first().trim().startsWith(":")
            ) {
                lines.removeFirst()
            }
            while (lines.firstOrNull()?.isBlank() == true) lines.removeFirst()
            require(lines.isNotEmpty() && lines.first().findIndent() > directiveIndent) {
                "Empty code block: ${directive.trim()}"
            }
            val code = mutableListOf<String>()
            while (
                lines.isNotEmpty() &&
                    (lines.first().isBlank() || lines.first().findIndent() > directiveIndent)
            ) {
                val line = lines.removeFirst()
                code += line
            }
            while (code.lastOrNull()?.isBlank() == true) code.removeLast()
            val indent = code.filter { it.isNotBlank() }.minOf { it.findIndent() }
            return CodeBlock(code.map { if (it.isBlank()) "" else it.drop(indent) })
        }

        private fun readIoBlock(lines: MutableList<String>): List<CodeBlock> {
            val indent = lines.removeFirst().findIndent()
            val blocks = mutableListOf<CodeBlock>()
            while (
                lines.isNotEmpty() &&
                    (lines.first().isBlank() || lines.first().findIndent() > indent)
            ) {
                val line = lines.first().trim()
                when {
                    line.isEmpty() || line.startsWith(":") -> lines.removeFirst()
                    line.startsWith(".. input::") || line.startsWith(".. output::") -> {
                        requireInlineTarget(line)
                        blocks += readBlock(lines)
                    }
                    else ->
                        throw UnsupportedOperationException(
                            "Unsupported io-code-block content: $line"
                        )
                }
            }
            require(blocks.isNotEmpty()) { "Empty io-code-block" }
            return blocks
        }

        private fun requireInlineTarget(line: String) {
            if (line.substringAfter("::").isNotBlank()) {
                throw UnsupportedOperationException(
                    "External snippet requires a source resolver: $line"
                )
            }
        }
    }

    enum class Type {
        DATA,
        ACTION,
        EXPECTED,
        INDEX,
    }

    var type: Type =
        when {
            isData() -> Type.DATA
            isAction() -> Type.ACTION
            isIndex() -> Type.INDEX
            else -> Type.EXPECTED
        }

    fun code(): List<String> = lines.toList()

    fun isAction(): Boolean =
        listOf(".aggregate(", ".find(", ".updateOne(", ".updateMany(").any(::contains)

    fun isData(): Boolean = contains(".insertOne(") || contains(".insertMany(")

    fun isIndex(): Boolean = contains(".createIndex(")

    fun isExpected(): Boolean = !isData() && !isAction() && !isIndex()

    private fun contains(text: String): Boolean = lines.any { it.contains(text) }
}
