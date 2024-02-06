package dev.morphia.audits.rst

import dev.morphia.audits.RstAuditor
import dev.morphia.audits.rst.Separator.DASH
import java.io.File

class RstDocument(lines: MutableList<String>) {
    companion object {
        val TABS_START = ".. tabs::"
        val FANCY_TAB_START = ".. tab::"
        val SIMPLE_TAB_START = "tabs:"

        fun read(file: File): RstDocument {
            return RstDocument(file.readLines().toMutableList())
        }
    }

    val exampleSection: Section

    init {
        exampleSection =
            lines
                .flatMap { line ->
                    if (line.trim().startsWith(".. include:: ")) {
                        val include = File(RstAuditor.includesRoot, line.substringAfter(":: "))
                        if (include.exists()) {
                            include.readLines()
                        } else listOf(line)
                    } else listOf(line)
                }
                .toMutableList()
                .sections()
                .first()
    }

    private fun List<String>.sections(): List<Section> {
        return DASH.partition(this)
            .filter { it.key in listOf("Example", "Examples") }
            .map { Section(it.key, it.value) }
    }

    fun tag(name: String): List<Tag> {
        return exampleSection.tag(name)
    }
}

fun <String> MutableList<String>.removeWhile(function: (String) -> Boolean): List<String> {
    val removed = mutableListOf<String>()
    while (isNotEmpty() && function(first())) {
        removed += removeFirst()
    }

    return removed
}
