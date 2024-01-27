package dev.morphia.audits.rst

import dev.morphia.audits.rst.Separator.DASH

class Section(input: MutableList<String>, val separator: Separator = DASH) {
    var name = "header"

    val tags: List<Tag>
    val examples = mutableListOf<OperatorExample>()

    init {
        tags = findTags(input)
        name = input.removeFirst()
        input.removeFirst()
        examples +=
            Separator.TILDE.partition(input)
                .map { OperatorExample(it, examples.lastOrNull(), separator.next()) }
                .flatMap { it.extractTabs() }
    }

    override fun toString(): String {
        return "Section($name)"
    }

    fun tag(name: String): List<Tag> {
        return tags.filter { it.type == name }
    }

    private fun findTags(lines: List<String>): List<Tag> {
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
}

fun MutableList<String>.subsections(separator: Separator): Map<String, MutableList<String>> {
    val sections = mutableMapOf<String, MutableList<String>>()
    var current = mutableListOf<String>()
    sections.put("main", current)
    val sectionSeparator = separator.next().section
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
