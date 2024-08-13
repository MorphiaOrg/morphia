package dev.morphia.critter.parser.ksp.extensions

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import dev.morphia.critter.parser.packageName
import dev.morphia.critter.parser.simpleName

fun KSClassDeclaration.toTypeName(): TypeName {
    return ClassName.get(packageName.asString(), simpleName.asString())
}

fun KSClassDeclaration.allAnnotations(): Set<KSAnnotation> {
    val types =
        superTypes
            .filter { it.simpleName() != "Any" }
            .map { it.resolve().declaration }
            .filterIsInstance<KSClassDeclaration>()
            .toList()
    var all =
        annotations.filter { it.annotationType.packageName().startsWith("dev.morphia") }.toSet() +
            types.flatMap { it.allAnnotations() }
    return all.distinctBy { it.shortName.asString() }.toSet()
}
/*

@OptIn(KspExperimental::class)
fun KSClassDeclaration.entityAnnotation() =
    try {
        allAnnotations()
            .first { it.annotationType.className() == Entity::class.java.name }
            .toEntity()
    } catch (e: NoSuchElementException) {
        throw MappingException("No Entity annotation found on ${name()}")
    }

private fun KSAnnotation.toEntity(): Entity {
    val map = arguments.map { it -> (it.name?.asString() ?: "value") to it.value }.toMap()
    val builder =
        EntityBuilder.entityBuilder()
            .value(map["value"].toString())
            .concern(map["concern"].toString())
            .discriminator(map["discriminator"].toString())
            .discriminatorKey(map["discriminatorKey"].toString())
            .useDiscriminator(map["useDiscriminator"] as Boolean)
    val ksAnnotation = map["cap"] as KSAnnotation
    builder.cap(ksAnnotation.toCappedAt())
    return builder.build()
}

private fun KSAnnotation.toCappedAt(): CappedAt {
    val map = arguments.map { it -> (it.name?.asString() ?: "value") to it.value }.toMap()
    val cappedAtBuilder = CappedAtBuilder.cappedAtBuilder()
    map["value"]?.let { cappedAtBuilder.value(it.toString().toLong()) }
    map["count"]?.let { cappedAtBuilder.count(it.toString().toLong()) }

    return cappedAtBuilder.build()
}
*/
