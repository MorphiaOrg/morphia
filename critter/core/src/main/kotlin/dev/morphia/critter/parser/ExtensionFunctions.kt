package dev.morphia.critter.parser

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference

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
