package dev.morphia.audits.rst

import dev.morphia.audits.RstAuditor
import dev.morphia.audits.findIndent
import dev.morphia.audits.rst.Separator.DASH
import dev.morphia.audits.rst.Separator.TILDE
import java.io.File

class RstDocument(val operator: String, lines: MutableList<String>) {
    companion object {
        val TABS_SECTION_START = ".. tabs::"
        val SIMPLE_TAB_SECTION_START = "tabs:"
        val FANCY_TAB_START = ".. tab::"
        val SIMPLE_TAB_START = "- id:"

        fun read(operator: String, file: File): RstDocument {
            val lines =
                file
                    .readLines()
                    .dropWhile { line -> line.trim() !in listOf("Example", "Examples") }
                    .drop(3)
                    .flatMap { line ->
                        if (line.trim().startsWith(".. include:: ")) {
                            val include = File(RstAuditor.includesRoot, line.substringAfter(":: "))
                            if (include.exists()) {
                                include.readLines()
                            } else listOf(line)
                        } else listOf(line)
                    }
                    .toMutableList()
            return RstDocument(operator, lines)
        }
    }

    var examples: List<OperatorExample> = mutableListOf()
        private set

    init {
        val partition = DASH.partition(lines).entries.last()
        val partitions = TILDE.partition(partition.value)
        partitions
            .map { it.value.extractTabs(it.key) }
            .flatMap { it.entries }
            .forEach {
                examples +=
                    OperatorExample(
                        examples.firstOrNull { it.dataBlock.isNotEmpty() },
                        operator,
                        it.key,
                        it.value
                    )
            }
        /*
                examples
                    .filter { it.dataBlock.isEmpty() }
                    .firstOrNull { it.name == "main" }
                    ?.let { main ->
                        examples
                            .filter { it.name != "main" }
                            .filter { it.dataBlock == null }
                            .forEach { it.dataBlock = main.dataBlock }
                    }
        */

        examples = examples.filter { it.valid() }
    }

    private fun MutableList<String>.extractTabs(name: String): Map<String, MutableList<String>> {
        val main = removeWhile { !it.startsWith(TABS_SECTION_START) }.toMutableList()
        val tabs = dedupeMap<String>()
        while (contains(TABS_SECTION_START)) {
            val localTabs = mutableMapOf<String, MutableList<String>>()
            removeFirst()
            removeWhile { it.isBlank() }
            extractFancyTabs(localTabs)
            extractSimpleTabs(localTabs)
            val appendix = removeWhile { !it.startsWith(TABS_SECTION_START) }.toMutableList()
            //            localTabs.values.forEach { it += appendix }
            localTabs.forEach {
                tabs["$name :: ${it.key}"] = (main + it.value + appendix).toMutableList()
            }
        }
        return if (tabs.isEmpty()) {
            mapOf(name to main)
        } else {
            //            val tabMaps = mutableMapOf<String, MutableList<String>>()
            //            tabs.forEach { entry ->
            //                entry.value.forEachIndexed { index, list ->
            //                    tabMaps.put(if (index == 0) entry.key else "${entry.key}
            // [${index}]", list)
            //                }
            //            }
            tabs
        }
    }

    private fun MutableList<String>.extractSimpleTabs(
        tabs: MutableMap<String, MutableList<String>>,
    ) {
        if (isNotEmpty() && first().trim().startsWith(SIMPLE_TAB_SECTION_START)) {
            removeFirst()
            removeWhile { it.isBlank() }
            while (isNotEmpty() && first().trim().startsWith(SIMPLE_TAB_START)) {
                val first = removeFirst()
                val indent = first.findIndent()
                var name = first.substringAfter(SIMPLE_TAB_START).trim()
                if (first().trim().startsWith("name: ")) {
                    name = first().substringAfter("name: ")
                }
                val list =
                    mutableListOf(first) +
                        removeWhile {
                            !first().trim().startsWith(SIMPLE_TAB_START) && it.atLeastIndent(indent)
                        }

                tabs[name] = list.toMutableList()
            }
        }
    }

    private fun MutableList<String>.extractFancyTabs(
        tabs: MutableMap<String, MutableList<String>>,
    ) {
        while (isNotEmpty() && first().trim().startsWith(FANCY_TAB_START)) {
            val first = removeFirst()
            val indent = first.findIndent()
            val name = first.substringAfter(FANCY_TAB_START).trim()
            val list = removeWhile {
                !first().trim().startsWith(FANCY_TAB_START) && it.atLeastIndent(indent)
            }

            tabs[name] = list.toMutableList()
        }
    }

    private fun <V> dedupeMap(): DedupeMap<MutableList<V>> = DedupeMap()

    fun String.atLeastIndent(indent: Int) = isBlank() || findIndent() >= indent

    class DedupeMap<V> : LinkedHashMap<String, V>() {
        override fun put(key: String, value: V): V? {
            var count = 1
            var newKey = key
            while (contains(newKey)) {
                newKey = "$key [${count++}]"
            }

            return super.put(newKey, value)
        }
    }
}

fun <String> MutableList<String>.removeWhile(function: (String) -> Boolean): List<String> {
    val removed = mutableListOf<String>()
    while (isNotEmpty() && function(first())) {
        removed += removeFirst()
    }

    return removed
}
