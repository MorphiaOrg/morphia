package dev.morphia.audits.rst

import dev.morphia.audits.model.CodeBlock
import dev.morphia.audits.model.CodeBlock.Companion.findBlocks
import dev.morphia.audits.model.CodeBlock.Type.ACTION
import dev.morphia.audits.model.CodeBlock.Type.DATA
import dev.morphia.audits.model.CodeBlock.Type.EXPECTED
import dev.morphia.audits.model.CodeBlock.Type.INDEX
import dev.morphia.audits.rst.Separator.TILDE

class OperatorExample(
    val name: String,
    input: List<String>,
    parent: OperatorExample? = null,
    separator: Separator = TILDE
) {
    companion object {
        fun hasTabs(input: List<String>) = input.any { it.startsWith(".. tabs::") }

        fun extractTabs(name: String, input: List<String>): Map<String, List<String>> {
            var tabName = name
            if (tabName.isBlank()) tabName = "main"
            return if (hasTabs(input)) {
                if (input.any { it.trim().startsWith(".. tab:: ") })
                    separateTabs(input, "   .. tab:: ")
                else separateTabs(input, "     - id: ")
            } else {
                mutableMapOf(tabName to input)
            }
        }

        private fun separateTabs(
            input: List<String>,
            separator: String
        ): Map<String, MutableList<String>> {
            val tabs = mutableMapOf<String, MutableList<String>>()
            val lines = input.toMutableList()
            val main = lines.removeWhile { !it.startsWith(".. tabs::") }.filter { it.isNotBlank() }
            lines.removeWhile { !it.startsWith(separator) }

            while (lines.isNotEmpty()) {
                val tab =
                    mutableListOf(lines.removeFirst()) +
                        lines.removeWhile { !it.startsWith(separator) }
                tabs[nameTab(tab, separator)] = (main + tab).toMutableList()
            }
            return tabs
        }

        private fun nameTab(tab: List<String>, separator: String): String {
            var name = tab[0].substringAfter(separator)
            if (tab[1].trim().startsWith("name: ")) name = tab[1].substringAfter("name: ")
            return name
        }
    }

    //    val testCases: List<OperatorExample>
    val actionBlock: CodeBlock?
    val dataBlock: CodeBlock?
    val expectedBlock: CodeBlock?
    val indexBlock: CodeBlock?

    init {
        /*
                testCases =
                    TILDE.partition(input)
                        .flatMap {
                            val tabs = extractTabs(name, it)
                            var index = 1
                            tabs.map {
                                OperatorExample("${it.key} #${index++}", it.value, this, separator.next())
                            }
                        }
        */

        val codeBlocks = findBlocks(input)
        actionBlock = codeBlocks[ACTION]?.removeFirstOrNull() ?: parent?.actionBlock
        dataBlock = codeBlocks[DATA]?.removeFirstOrNull() ?: parent?.dataBlock
        expectedBlock = codeBlocks[EXPECTED]?.removeFirstOrNull() ?: parent?.expectedBlock
        indexBlock = codeBlocks[INDEX]?.removeFirstOrNull() ?: parent?.indexBlock
    }
}
