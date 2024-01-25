package dev.morphia.audits.rst

import dev.morphia.audits.RstAuditor
import java.io.File

class RstDocument(lines: MutableList<String>) {
    companion object {
        fun read(file: File): RstDocument {
            return RstDocument(file.readLines().toMutableList())
        }
    }

    val sections: Map<String, Section>

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
                .map { it.name to it }
                .toMap()
    }

    private fun MutableList<String>.sections(): List<Section> {
        var sections = mutableListOf<Section>()
        while (isNotEmpty()) {
            sections += Section(this)
        }

        return sections.reversed()
    }

    fun tag(name: String): Tag? {
        return sections.values.map { it.tag(name) }.firstOrNull()
    }

    fun examples() = (sections["Examples"] ?: sections["Example"]) as Section
}
