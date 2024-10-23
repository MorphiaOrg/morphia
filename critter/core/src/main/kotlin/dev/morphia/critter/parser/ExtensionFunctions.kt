package dev.morphia.critter.parser

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import java.util.Locale

fun KSDeclaration.packageName() = packageName.asString()

fun KSDeclaration.simpleName() = simpleName.asString()

fun KSTypeReference.packageName(): String {
    val declaration = resolve().declaration
    declaration.qualifiedName
    return declaration.packageName()
}

fun KSTypeReference.simpleName(): String {
    return resolve().declaration.simpleName()
}

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
