package dev.morphia.audits.rst

import dev.morphia.audits.findIndent
import dev.morphia.audits.model.CodeBlock
import dev.morphia.audits.model.CodeBlock.Companion.findBlocks
import dev.morphia.audits.model.CodeBlock.Type.ACTION
import dev.morphia.audits.model.CodeBlock.Type.DATA
import dev.morphia.audits.model.CodeBlock.Type.EXPECTED
import dev.morphia.audits.model.CodeBlock.Type.INDEX

class OperatorExample(
    val name: String,
    private val input: List<String>,
//    parent: OperatorExample? = null,
//    separator: Separator = TILDE
) {
    companion object {
        fun String.sanitize(): String {
            return replace("`", "")
        }

        fun hasTabs(input: List<String>) = input.any { it.startsWith(".. tabs::") }

        fun extractTabs(name: String, input: List<String>): Map<String, List<String>> {
            var tabName = name.sanitize()
            if (tabName.isBlank()) tabName = "main"
            val map =
                if (hasTabs(input)) {
                    separateTabs(
                        tabName,
                        input,
                        if (fancy(input)) "   .. tab:: " else "     - id: "
                    )
                    //                if (input.any { it.trim().startsWith(".. tab:: ") })
                    //                    separateTabs(tabName, input, "   .. tab:: ")
                    //                else separateTabs(tabName, input, "     - id: ")
                } else {
                    mutableMapOf(tabName to input)
                }
            return map
        }

        private fun fancy(input: List<String>) = input.any { it.trim().startsWith(".. tab:: ") }

        private fun separateTabs(
            tabName: String,
            input: List<String>,
            separator: String
        ): Map<String, MutableList<String>> {
            val tabs = mutableMapOf<String, MutableList<String>>()
            val lines = input.toMutableList()
            val main = lines.removeWhile { !it.startsWith(".. tabs::") }
            val tabSections = mutableListOf<MutableList<String>>()
            while (lines.contains(".. tabs::")) {
                lines.removeFirst()
                tabSections += lines.removeWhile { !it.startsWith(".. tabs::") }.toMutableList()
            }
            tabSections.forEach { tabSection ->
                tabSection.removeWhile { !it.startsWith(separator) }
                val indent = tabSection.first().findIndent()
                while (tabSection.isNotEmpty() && tabSection.first().findIndent() >= indent) {
                    val tab =
                        mutableListOf(tabSection.removeFirst()) +
                            tabSection.removeWhile {
                                !it.startsWith(separator) &&
                                    (tabSection.first().isBlank() ||
                                        tabSection.first().findIndent() >= indent)
                            }
                    var name = nameTab(tabName, tab, separator)
                    var count = 0
                    while (tabs.containsKey(name)) {
                        count++
                        name = nameTab(tabName, tab, separator) + " [$count]"
                    }
                    tabs[name] = (main + tab).toMutableList()
                }
            }
            return tabs
        }

        private fun nameTab(tabName: String, tab: List<String>, separator: String): String {
            var name = tab[0].substringAfter(separator)
            if (tab[1].trim().startsWith("name: ")) name = tab[1].substringAfter("name: ")
            return "$tabName :: $name tab"
        }
    }

    //    val testCases: List<OperatorExample>
    val actionBlock: CodeBlock?
    val dataBlock: CodeBlock?
    val expectedBlock: CodeBlock?
    val indexBlock: CodeBlock?

    init {
        val codeBlocks = findBlocks(input)
        actionBlock = codeBlocks[ACTION]?.removeFirstOrNull() // ?: parent?.actionBlock
        dataBlock = codeBlocks[DATA]?.removeFirstOrNull() // ?: parent?.dataBlock
        expectedBlock = codeBlocks[EXPECTED]?.removeFirstOrNull() // ?: parent?.expectedBlock
        indexBlock = codeBlocks[INDEX]?.removeFirstOrNull() // ?: parent?.indexBlock
    }

    override fun toString() =
        "OperatorExample(name='$name', actionBlock: ${actionBlock != null}, dataBlock: ${dataBlock != null}, " +
            "expectedBlock=${expectedBlock != null}, " +
            "indexBlock: ${indexBlock != null})"
}
