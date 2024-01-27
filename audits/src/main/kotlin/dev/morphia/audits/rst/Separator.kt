package dev.morphia.audits.rst

import com.antwerkz.expression.RegularExpression
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

    fun partition(lines: MutableList<String>): List<MutableList<String>> {
        val partitions = mutableListOf(mutableListOf<String>())
        while (lines.isNotEmpty()) {
            if (!section.matches(lines.first())) {
                partitions.last() += lines.removeFirst()
            } else {
                var next = mutableListOf(partitions.last().removeLast())
                next += lines.removeFirst()
                partitions += next
            }
        }

        return partitions
    }
}
