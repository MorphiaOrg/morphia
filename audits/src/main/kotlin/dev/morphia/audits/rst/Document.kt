package dev.morphia.audits.rst

import com.antwerkz.expression.RegularExpression
import com.antwerkz.expression.toRegex
import dev.morphia.audits.RstAuditor.Companion.includesRoot
import dev.morphia.audits.findIndent
import dev.morphia.audits.model.CodeBlock
import dev.morphia.audits.notControl
import java.io.File

class Document(source: File) {
    private var header: String = ""
    val elements: MutableList<Section> = mutableListOf()

    init {
        val lines = source.readLines().toMutableList()
        var section: Section = Header()
        elements += section
        if (lines[0].startsWith("===")) {
            lines.drop()
            header = lines.removeWhile { !it.startsWith("===") }.joinToString("\n")
            lines.drop()
        }

        val sectionMarker =
            RegularExpression.startOfInput().atLeast(3) { anyOfChars("-~") }.toRegex()
        val tagMarker = Regex("^\\s*:(?<tag>\\w+):(?: (?<value>.+))*$")
        val directiveMarker = Regex("^\\s*\\.\\. (?<directive>.+)::(?: (?<value>.+))*\$")

        var line = ""
        while (lines.isNotEmpty()) {
            line = lines.next()
            when {
                line.matches(directiveMarker) -> {
                    when (directiveMarker.find(line)?.namedCapture("directive")) {
                        "include" -> lines.prepend(include(line))
                        "code-block" -> section += CodeBlock.readBlock(lines)
                        "io-code-block" -> section += readIoCodeBlock(lines)
                        else -> section += TextElement(line)
                    }
                }
                line.matches(tagMarker) -> {
                    tagMarker.find(line)?.let { result ->
                        result.groups["tag"]?.value?.let {
                            section.tag(it, result.groups["value"]?.value)
                        }
                    }
                }
                line.matches(sectionMarker) -> {
                    val name = section.elements.removeLast() as TextElement
                    section = Section(name.line, section, subsection = !line.startsWith("-"))
                    elements += section
                }
                else -> section += TextElement(line)
            }
        }
    }

    private fun readIoCodeBlock(lines: MutableList<String>): List<CodeBlock> {
        val indent = lines.first().findIndent()
        lines.removeWhile { it.trim().startsWith(":") || it.isBlank() }
        val blocks = mutableListOf<CodeBlock>()
        var block: CodeBlock? = null
        while (
            lines.isNotEmpty() && (lines.first().findIndent() >= indent || lines.first().isBlank())
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

    private fun include(line: String): List<String> {
        val include = File(includesRoot, line.substringAfter(":: "))
        return if (include.exists()) {
            include.readLines()
        } else {
            loadInclude(line.substringAfterLast("/").substringBefore(".")) ?: listOf(line)
        }
    }

    private fun loadInclude(keyword: String) =
        File(includesRoot, "includes")
            .listFiles()
            ?.filter { file -> file.isFile }
            ?.mapNotNull { file -> loadReference(keyword, file) }
            ?.firstOrNull()

    private fun loadReference(keyword: String, file: File): List<String>? {
        var lines = file.readLines().dropWhile { line -> !line.contains("ref: $keyword") }.drop(1)
        if (lines.isEmpty()) {
            return null
        }
        lines = lines.dropWhile { !it.startsWith("content") && !it.startsWith("---") }
        if (!lines[0].startsWith("content")) {
            return null
        }
        val removeWhile =
            lines.drop(1).toMutableList().removeWhile { it.isBlank() || it.startsWith("  ") }
        return removeWhile
    }

    fun exampleSections(): List<Section> {
        val examples = mutableListOf<Section>()
        var index = elements.indexOfFirst { it.name.startsWith("Example") }
        examples += elements[index++] as Section
        while (index < elements.size && elements[index++].subsection) {
            examples += elements[index - 1]
        }
        return examples
    }
}

fun MatchResult.namedCapture(groupName: String) =
    groups[groupName]?.value // ?: throw IllegalStateException("it matched already")

fun MutableList<String>.drop() = removeAt(0)

private fun MutableList<String>.next(): String {
    //    removeWhile { it.isBlank() }
    return drop()
}

private fun MutableList<String>.prepend(lines: List<String>) {
    if (isNotEmpty()) {
        addAll(0, lines)
    } else {
        addAll(lines)
    }
}

open class Element {
    val elements = mutableListOf<Element>()

    operator fun plusAssign(element: Element) {
        elements += element
    }

    operator fun plusAssign(elements: List<Element>) {
        this.elements += elements
    }
}

class Header : Section("header")

open class Section(
    val name: String,
    val previous: Section? = null,
    val subsection: Boolean = false
) : Element() {
    private val tags = mutableMapOf<String, String?>()

    fun tag(name: String, value: String?) {
        tags[name] = value
    }

    override fun toString(): String {
        return "Section(name='$name')"
    }
}

operator fun Section.plusAssign(block: CodeBlock) {
    this += CodeBlockElement(block)
}

operator fun Section.plusAssign(blocks: List<CodeBlock>) {
    this += blocks.map { block -> CodeBlockElement(block) }
}

data class CodeBlockElement(val block: CodeBlock) : Element()

data class TextElement(val line: String) : Element()
