package dev.morphia.critter.parser.ksp.extensions

import com.google.devtools.ksp.symbol.KSDeclaration
import dev.morphia.critter.parser.packageName
import dev.morphia.critter.parser.simpleName

fun KSDeclaration.name() = simpleName.asString()

fun KSDeclaration.methodName() = name().first().lowercase() + name().substring(1)

fun KSDeclaration.className() = (qualifiedName ?: simpleName).asString()

fun KSDeclaration.packageName() = packageName.asString()

// fun KSClassDeclaration.codecPackageName() = "${DEFAULT_PACKAGE}.${name().snakeCase()}"

fun KSDeclaration.simpleName() = simpleName.asString()
