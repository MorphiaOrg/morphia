package dev.morphia.audits.rst

import dev.morphia.audits.RstAuditor
import java.io.File

class RstDocument(lines: MutableList<String>) {
    companion object {
        fun read(file: File): RstDocument {
            return RstDocument(file.readLines().toMutableList())
        }
    }

    val sections: List<Section>

    init {
        sections =
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
    }

    private fun MutableList<String>.sections(): List<Section> {
        var sections = mutableListOf<Section>()
        while (isNotEmpty()) {
            sections += Section(this)
        }

        return sections.reversed()
    }

    fun tag(name: String): Tag? {
        return sections.map { it.tag(name) }.firstOrNull()
    }

    fun examples() = sections.first { it.name == "Examples" || it.name == "Example" }
}
