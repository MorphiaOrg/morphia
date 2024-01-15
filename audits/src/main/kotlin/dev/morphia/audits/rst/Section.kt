package dev.morphia.audits.rst

import dev.morphia.audits.findIndent
import dev.morphia.audits.model.CodeBlock
import dev.morphia.audits.notControl
import dev.morphia.audits.removeWhile

class Section(lines: MutableList<String>) {
    private var lines: List<String>
    var name = "header"
    val tags: List<Tag> by lazy { findTags() }
    val codeBlocks: List<CodeBlock> by lazy { findBlocks() }
    var actionBlock: CodeBlock?
        private set

    var dataBlock: CodeBlock?
        private set

    var expectedBlock: CodeBlock?
        private set

    init {
        var sectionLines = mutableListOf<String>()
        while (lines.isNotEmpty() && !lines.last().startsWith("---")) {
            sectionLines += lines.removeLast()
        }
        if (lines.isNotEmpty()) {
            lines.removeLast()
            name = lines.removeLast()
        }

        this.lines = sectionLines.asReversed()
        dataBlock = dataBlock()
        actionBlock = actionBlock()
        expectedBlock = expectedBlock()
    }

    constructor(parent: Section, lines: MutableList<String>) : this(lines) {
        if (dataBlock == null) {
            dataBlock = parent.dataBlock
        }
        if (actionBlock == null) {
            actionBlock = parent.actionBlock
        }
        if (expectedBlock == null) {
            expectedBlock = parent.expectedBlock
        }
    }

    override fun toString(): String {
        return "Section($name, $tags)"
    }

    fun tag(name: String): Tag? {
        return tags.firstOrNull { it.type == name }
    }

    private fun findBlocks(): List<CodeBlock> {
        val sections = lines.subsections()
        var blocks = mutableListOf<CodeBlock>()

        sections.forEach { name, data ->
            var line = ""
            while (data.isNotEmpty()) {
                line = data.removeFirst().trim()
                if (line.trim().startsWith(".. code-block:: ")) {
                    blocks += readBlock(data)
                }
            }
        }

        return blocks
    }

    private fun readBlock(lines: MutableList<String>): CodeBlock {
        lines.removeWhile { !notControl(it) || it.isBlank() }.toMutableList()
        var block = CodeBlock()
        block.indent = lines.first().findIndent()
        while (
            lines.isNotEmpty() &&
                (lines.first().findIndent() >= block.indent || lines.first().isBlank())
        ) {
            block += lines.removeFirst()
        }
        return block
    }

    private fun findTags(): List<Tag> {
        val list = mutableListOf<Tag>()
        lines.forEachIndexed { index, line ->
            if (line.startsWith(".. ") && !line.startsWith(".. code-block")) {
                val tag = Tag(line.substringBefore("::").substring(3), line.substringAfter(":: "))
                list += tag
                var position = index
                while (++position < lines.size && lines[position].startsWith("   :")) {
                    tag += lines[position].trim()
                }
            }
        }

        return list
    }

    fun subsections(): List<Section> {
        val subsections = lines.subsections()
        return subsections
            .map { (name, lines) -> Section(lines).also { section -> section.name = name } }
            .flatMap { section ->
                if (section.hasTabs()) {
                    section.extractTabs()
                } else {
                    listOf(section)
                }
            }
    }

    private fun extractTabs(): List<Section> {
        return if (lines.any { it.trim().startsWith(".. tab:: ") }) fancyTabs() else basicTabs()
    }

    private fun fancyTabs(): List<Section> {
        val list = mutableListOf<Section>()
        var lines = this.lines.toMutableList()
        while (lines.isNotEmpty() && !lines[0].startsWith(".. tabs::")) {
            lines.removeFirst()
        }
        if (lines.isNotEmpty()) {
            lines.removeFirst()
            if (lines[0].isBlank()) {
                lines.removeFirst()
            }
            val tabs = separateTabs(lines, ".. tab:: ")

            println("**************** tabs.size = ${tabs.size}")
            list +=
                tabs.map { tab ->
                    Section(this, tab.toMutableList()).also {
                        it.name = "${this.name} - ${tab.first().substringAfter("tab:: ")} tab"
                    }
                }
        }
        return list
    }

    private fun basicTabs(): MutableList<Section> {
        val list = mutableListOf<Section>()
        var lines = this.lines.toMutableList()
        while (lines.isNotEmpty() && !lines[0].startsWith("   tabs:")) {
            lines.removeFirst()
        }
        if (lines.isNotEmpty()) {
            lines.removeFirst()
            if (lines[0].isBlank()) {
                lines.removeFirst()
            }
            val tabs = separateTabs(lines, "- id: ")
            println("**************** tabs.size = ${tabs.size}")
            list +=
                tabs.map { tab ->
                    Section(tab.toMutableList()).also {
                        it.name = "${this.name} - ${tab.first().substringAfter("id: ")} tab"
                    }
                }
        }
        return list
    }

    private fun separateTabs(
        lines: MutableList<String>,
        separator: String
    ): MutableList<List<String>> {
        val tabs = mutableListOf<List<String>>()
        while (lines.isNotEmpty()) {
            if (lines[0].trim().startsWith(separator)) {
                val indent = lines[0].findIndent()
                val tab = mutableListOf(lines.removeFirst())
                tabs += tab
                while (lines.isNotEmpty() && isTabLine(lines, indent, separator)) {
                    tab += lines.removeFirst()
                }
            } else {
                lines.removeFirst()
            }
        }
        return tabs
    }

    private fun isTabLine(lines: MutableList<String>, indent: Int, prefix: String) =
        lines[0].isBlank() || lines[0].findIndent() > indent && !lines[0].trim().startsWith(prefix)

    private fun hasTabs() = lines.any { it.startsWith(".. tabs::") }

    private fun List<String>.subsections(): Map<String, MutableList<String>> {
        var sections = mutableMapOf<String, MutableList<String>>()
        var current = mutableListOf<String>()
        sections.put("main", current)
        forEach {
            if (it.startsWith("~~~")) {
                val name = current.removeLast()
                current = mutableListOf()
                sections.put(name, current)
            } else {
                current.add(it)
            }
        }

        return sections
    }

    private fun dataBlock(): CodeBlock? {
        val dataBlock = codeBlocks.firstOrNull { it.isData() }
        return when {
            dataBlock != null -> dataBlock
            else -> {
                val actionBlock = actionBlock()
                if (actionBlock == null) {
                    findBlocks().firstOrNull()
                } else {
                    val blocks = codeBlocks
                    blocks
                        .filterIndexed { index, codeBlock ->
                            index + 1 < blocks.size && blocks[index + 1] == actionBlock
                        }
                        .firstOrNull()
                }
            }
        }
    }

    private fun expectedBlock(): CodeBlock? {
        val actionBlock = actionBlock()
        val blocks = codeBlocks
        return blocks
            .filterIndexed { index, codeBlock -> index > 0 && blocks[index - 1] == actionBlock }
            .firstOrNull()
    }

    private fun actionBlock(): CodeBlock? {
        return codeBlocks.firstOrNull { it.isAction() }
    }
}
