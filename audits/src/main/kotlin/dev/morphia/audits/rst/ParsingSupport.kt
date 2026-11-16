package dev.morphia.audits.rst

internal fun String.findIndent(): Int = takeWhile { it.isWhitespace() }.length
