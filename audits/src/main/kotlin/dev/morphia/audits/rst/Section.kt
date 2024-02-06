package dev.morphia.audits.rst

import dev.morphia.audits.rst.OperatorExample.Companion.extractTabs
import dev.morphia.audits.rst.Separator.DASH
import dev.morphia.audits.rst.Separator.TILDE

class Section(val name: String, input: List<String>, val separator: Separator = DASH) {

    val tags: List<Tag>
    var examples = mutableListOf<OperatorExample>()
        private set

    init {
        tags = findTags(input)
        val partitions = TILDE.partition(input)
        val map = partitions.flatMap { extractTabs(it.key, it.value).entries }
        examples +=
            map.map {
                OperatorExample(it.key, it.value, examples.lastOrNull(), separator.next())
                //                    extractTabs(it).map {
                //                    }
            }
        //                .map { OperatorExample(it, examples.lastOrNull(), separator.next()) }
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
