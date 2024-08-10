package dev.morphia.critter.parser.ksp.extensions

import java.util.Locale

fun String.titleCase(): String {
    return first().uppercase(Locale.getDefault()) + substring(1)
}

fun String.methodCase(): String {
    return first().lowercase(Locale.getDefault()) + substring(1)
}

private val snakeCaseRegex = Regex("(?<=.)[A-Z]")

fun String.snakeCase(): String {
    return snakeCaseRegex.replace(this, "_$0").lowercase(Locale.getDefault())
}
