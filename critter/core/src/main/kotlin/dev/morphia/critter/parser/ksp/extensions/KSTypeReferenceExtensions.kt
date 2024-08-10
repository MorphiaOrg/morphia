package dev.morphia.critter.parser.ksp.extensions

import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability.NULLABLE

fun KSTypeReference.nullable() = resolve().nullability == NULLABLE

fun KSTypeReference.packageName(): String {
    return resolve().declaration.packageName()
}

fun KSTypeReference.className(): String {
    return resolve().declaration.className()
}

fun KSTypeReference.simpleName(): String {
    return resolve().declaration.simpleName()
}
