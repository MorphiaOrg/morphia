@file:OptIn(ExperimentalStdlibApi::class)

package dev.morphia.audits.rst

import com.antwerkz.expression.RegularExpression
import com.antwerkz.expression.RegularExpression.Companion.char
import com.antwerkz.expression.toRegex

enum class Separator(val separator: Char) {
    DASH('-'),
    TILDE('~'),
    TICK('`'),
    TERMINAL(0.toChar());

    fun next(): Separator {
        return if (ordinal < entries.size) entries[ordinal + 1] else TERMINAL
    }

    val section = let { RegularExpression.startOfInput().atLeast(5) { char(separator) } }.toRegex()

    fun partition(input: List<String>): Map<String, MutableList<String>> {
        var name = "main"
        val partitions = mutableMapOf(name to mutableListOf<String>())
        val lines = input.toMutableList()
        while (lines.isNotEmpty()) {
            partitions[name]?.plusAssign(lines.removeWhile { !section.matches(lines.first()) })
            if (lines.isNotEmpty()) {
                name = partitions[name]!!.removeLast()
                lines.removeFirst()
                partitions[name] = mutableListOf()
            }
        }

        return partitions
    }
}
