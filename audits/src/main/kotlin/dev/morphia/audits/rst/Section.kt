package dev.morphia.audits.rst

import com.antwerkz.expression.RegularExpression
import com.antwerkz.expression.toRegex
import dev.morphia.audits.findIndent
import dev.morphia.audits.model.CodeBlock
import dev.morphia.audits.notControl
import dev.morphia.audits.removeWhile
import dev.morphia.audits.rst.Section.Companion.SEPARATOR.DASH

class Section(
    lines: MutableList<String>,
    val parent: Section? = null,
    val separator: SEPARATOR = DASH
) {
    companion object {
        fun sectionSeparator(separator: SEPARATOR) =
            (separator?.let {
                    RegularExpression.startOfInput().atLeast(5) { char(separator.separator) }
                }
                    ?: RegularExpression.endOfInput().startOfInput())
                .toRegex()

        enum class SEPARATOR(val separator: Char) {
            DASH('-'),
            TILDE('~'),
            TICK('`'),
            TERMINAL(0.toChar());

            fun next(): SEPARATOR {
                return if (ordinal < entries.size) entries[ordinal + 1] else TERMINAL
            }
        }
    }

    var name = "header"
    val tags: List<Tag> by lazy { findTags() }
    val actionBlock: CodeBlock? by lazy { actionBlock() }
    val dataBlock: CodeBlock? by lazy { dataBlock() }
    val expectedBlock: CodeBlock? by lazy { expectedBlock() }
    private var lines = mutableListOf<String>()
    private val codeBlocks: List<CodeBlock> by lazy { findBlocks() }

    init {
        val regex = sectionSeparator(separator)

        while (lines.isNotEmpty() && !regex.matches(lines.last())) this.lines += lines.removeLast()
        if (lines.isNotEmpty()) {
            lines.removeLast()
            name = lines.removeLast()
        }

        this.lines.reverse()
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
        val map =
            subsections.map { (name, lines) ->
                Section(lines, this, separator.next()).also { section -> section.name = name }
            }
        return map.flatMap { section ->
            if (section.hasTabs()) {
                section.extractTabs()
            } else {
                listOf(section)
            }
        }
    }

    private fun extractTabs(): List<Section> {
        val tabs = mutableListOf<Section>()
        while (hasTabs()) {
            tabs +=
                if (lines.any { it.trim().startsWith(".. tab:: ") })
                    separateTabs(lines, "   .. tab:: ")
                else separateTabs(lines, "     - id: ")
        }

        return tabs.reversed()
    }

    private fun separateTabs(lines: MutableList<String>, separator: String): List<Section> {
        val tabs = mutableListOf<Section>()
        var tabLines = mutableListOf<String>()
        while (lines.isNotEmpty() && lines.last().trim() != ".. tabs::") {
            if (!lines.last().startsWith(separator)) {
                tabLines += lines.removeLast()
            } else {
                tabLines += lines.removeLast()
                tabs +=
                    Section(tabLines.reversed().toMutableList(), this, this.separator.next()).also {
                        it.name = "$name - ${it.lines.first().substringAfter(separator).trim()} tab"
                    }
                tabLines = mutableListOf()
            }
        }
        if (lines.isNotEmpty() && lines.last().trim() == ".. tabs::") lines.removeLast()
        return tabs
    }

    private fun isTabLine(lines: MutableList<String>, indent: Int, prefix: String) =
        lines[0].isBlank() || lines[0].findIndent() > indent && !lines[0].trim().startsWith(prefix)

    private fun hasTabs() = lines.any { it.startsWith(".. tabs::") }

    private fun List<String>.subsections(): Map<String, MutableList<String>> {
        var sections = mutableMapOf<String, MutableList<String>>()
        var current = mutableListOf<String>()
        sections.put("main", current)
        val sectionSeparator = sectionSeparator(separator.next())
        forEach {
            if (sectionSeparator.matches(it)) {
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
