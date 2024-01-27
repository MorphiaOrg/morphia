package dev.morphia.audits.rst

import dev.morphia.audits.findIndent
import dev.morphia.audits.model.CodeBlock
import dev.morphia.audits.model.CodeBlock.Type
import dev.morphia.audits.model.CodeBlock.Type.ACTION
import dev.morphia.audits.model.CodeBlock.Type.DATA
import dev.morphia.audits.model.CodeBlock.Type.EXPECTED
import dev.morphia.audits.model.CodeBlock.Type.INDEX
import dev.morphia.audits.notControl
import dev.morphia.audits.removeWhile
import dev.morphia.audits.rst.Separator.TILDE

class OperatorExample(
    private val input: MutableList<String>,
    parent: OperatorExample? = null,
    separator: Separator = TILDE
) {
    companion object {
        fun findBlocks(
            lines: MutableList<String>,
            separator: Separator
        ): Map<Type, MutableList<CodeBlock>> {
            val sections = lines.subsections(separator)
            val blocks = mutableListOf<CodeBlock>()

            sections.forEach { name, data ->
                var line: String
                while (data.isNotEmpty()) {
                    line = data.removeFirst().trim()
                    if (line.trim().startsWith(".. code-block:: ")) {
                        blocks += readBlock(data)
                    }
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

        private fun readBlock(lines: MutableList<String>): CodeBlock {
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

    val actionBlock: CodeBlock?
    val dataBlock: CodeBlock?
    val expectedBlock: CodeBlock?
    val indexBlock: CodeBlock?
    val name = parent.name

    init {
        val codeBlocks = findBlocks(input, separator)
        actionBlock = codeBlocks[ACTION]?.removeFirstOrNull() ?: parent?.actionBlock
        dataBlock = codeBlocks[DATA]?.removeFirstOrNull() ?: parent?.dataBlock
        expectedBlock = codeBlocks[EXPECTED]?.removeFirstOrNull() ?: parent?.expectedBlock
        indexBlock = codeBlocks[INDEX]?.removeFirstOrNull() ?: parent?.indexBlock
    }

    private fun hasTabs() = input.any { it.startsWith(".. tabs::") }

    fun extractTabs(): List<OperatorExample> {
        val tabs = mutableListOf<OperatorExample>()
        if (hasTabs()) {
            /*
                        while (hasTabs()) {
                            tabs +=
                                if (input.any { it.trim().startsWith(".. tab:: ") })
                                    separateTabs(input, "   .. tab:: ")
                                else separateTabs(input, "     - id: ")
                        }
            */
        } else {
            tabs += this
        }

        return tabs
    }

    /*
        private fun separateTabs(lines: MutableList<String>, separator: String): List<OperatorExample> {
            val tabs = mutableListOf<OperatorExample>()
            var tabLines = mutableListOf<String>()
            lines.removeWhile { !it.startsWith(separator) }
            val index = lines.indexOfFirst { it.startsWith(separator) }

            val indent = lines[index].findIndent()
            tabLines += lines.removeAt(index)
            while (
                index < lines.size && (lines[index].isBlank() || lines[index].findIndent() >= indent)
            ) {
                if (!lines[index].startsWith(separator)) {
                    tabLines += lines.removeAt(index)
                } else {
                    tabs +=
                        Section(tabLines, this, this.separator.next()).also {
                            it.name = "$name - ${it.lines.first().substringAfter(separator).trim()} tab"
                        }
                    tabLines = mutableListOf(lines.removeAt(index))
                }
            }
            if (tabLines.isNotEmpty() && tabLines.first().startsWith(separator))
                tabs +=
                    Section(tabLines, this, this.separator.next()).also {
                        it.name = "$name - ${it.lines.first().substringAfter(separator).trim()} tab"
                    }
            return tabs
        }
    */

}
