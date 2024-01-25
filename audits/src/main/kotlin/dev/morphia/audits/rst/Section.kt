package dev.morphia.audits.rst

import com.antwerkz.expression.RegularExpression
import com.antwerkz.expression.toRegex
import dev.morphia.audits.findIndent
import dev.morphia.audits.model.CodeBlock
import dev.morphia.audits.model.CodeBlock.Type
import dev.morphia.audits.model.CodeBlock.Type.ACTION
import dev.morphia.audits.model.CodeBlock.Type.DATA
import dev.morphia.audits.model.CodeBlock.Type.EXPECTED
import dev.morphia.audits.model.CodeBlock.Type.INDEX
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
            separator
                .let { RegularExpression.startOfInput().atLeast(5) { char(separator.separator) } }
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
    val actionBlock: CodeBlock? by lazy {
        codeBlocks[ACTION]?.removeFirstOrNull() ?: parent?.actionBlock
    }
    val dataBlock: CodeBlock? by lazy { codeBlocks[DATA]?.removeFirstOrNull() ?: parent?.dataBlock }
    val expectedBlock: CodeBlock? by lazy {
        codeBlocks[EXPECTED]?.removeFirstOrNull() ?: parent?.expectedBlock
    }
    val indexBlock: CodeBlock? by lazy {
        codeBlocks[INDEX]?.removeFirstOrNull() ?: parent?.indexBlock
    }
    private var lines = mutableListOf<String>()
    private val codeBlocks: Map<Type, MutableList<CodeBlock>> by lazy { findBlocks() }

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

    fun findBlocks(): Map<Type, MutableList<CodeBlock>> {
        val sections = lines.subsections()
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

        return tabs
    }

    private fun separateTabs(lines: MutableList<String>, separator: String): List<Section> {
        val tabs = mutableListOf<Section>()
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

    private fun hasTabs() = lines.any { it.startsWith(".. tabs::") }

    private fun MutableList<String>.subsections(): Map<String, MutableList<String>> {
        val sections = mutableMapOf<String, MutableList<String>>()
        var current = mutableListOf<String>()
        sections.put("main", current)
        val sectionSeparator = sectionSeparator(separator.next())
        while (isNotEmpty()) {
            val first = removeFirst()
            if (sectionSeparator.matches(first)) {
                val name = current.removeLast()
                current = mutableListOf()
                sections.put(name, current)
            } else {
                current.add(first)
            }
        }

        return sections
    }
}
