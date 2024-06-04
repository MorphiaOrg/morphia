@file:Suppress("UNCHECKED_CAST")

package dev.morphia.critter.conventions

import dev.morphia.annotations.Entity
import dev.morphia.annotations.ExternalEntity
import dev.morphia.annotations.internal.AnnotationFactory
import dev.morphia.annotations.internal.EntityBuilder
import dev.morphia.annotations.internal.MorphiaInternal
import dev.morphia.critter.parser.booleanMethod
import dev.morphia.critter.parser.stringMethod
import dev.morphia.mapping.Mapper
import dev.morphia.mapping.MappingException
import io.github.dmlloyd.classfile.AnnotationValue.OfBoolean
import io.github.dmlloyd.classfile.AnnotationValue.OfByte
import io.github.dmlloyd.classfile.AnnotationValue.OfCharacter
import io.github.dmlloyd.classfile.AnnotationValue.OfDouble
import io.github.dmlloyd.classfile.AnnotationValue.OfFloat
import io.github.dmlloyd.classfile.AnnotationValue.OfInteger
import io.github.dmlloyd.classfile.AnnotationValue.OfLong
import io.github.dmlloyd.classfile.AnnotationValue.OfString
import io.github.dmlloyd.classfile.ClassBuilder
import io.github.dmlloyd.classfile.ClassModel
import io.github.dmlloyd.classfile.attribute.RuntimeVisibleAnnotationsAttribute
import java.lang.constant.ClassDesc
import java.lang.reflect.Proxy

/** A set of conventions to apply to Morphia entities */
@MorphiaInternal
class CritterDefaultsConvention : CritterConvention {
    override fun apply(mapper: Mapper, builder: ClassBuilder, model: ClassModel) {
        val entity =
            model.getAnnotation(ExternalEntity::class.java)?.let { externalEntity ->
                EntityBuilder.entityBuilder()
                    .cap(externalEntity.cap)
                    .concern(externalEntity.concern)
                    .discriminator(externalEntity.discriminator)
                    .discriminatorKey(externalEntity.discriminatorKey)
                    .value(externalEntity.value)
                    .useDiscriminator(externalEntity.useDiscriminator)
                    .build()
            }
                ?: model.getAnnotation(Entity::class.java) as Entity?
                ?: throw MappingException("no @Entity or @ExternalEntity")

        val config = mapper.config
        builder.booleanMethod("discriminatorEnabled", entity.useDiscriminator)
        builder.stringMethod(
            "discriminatorKey",
            applyDefaults(entity.discriminatorKey, config.discriminatorKey())
        )

        //
        //        config.discriminator().apply(model)
    }

    fun applyDefaults(configured: String, defaultValue: String): String {
        return if (configured != Mapper.IGNORED_FIELDNAME) {
            configured
        } else {
            defaultValue
        }
    }
}

private fun <A : Annotation> ClassModel.getAnnotation(annotation: Class<in A>): A? {
    val ann =
        this.attributes()
            .filterIsInstance<RuntimeVisibleAnnotationsAttribute>()
            .map { a ->
                a.annotations().firstOrNull {
                    it.classSymbol()
                        .equals(ClassDesc.of(annotation.packageName, annotation.simpleName))
                }
            }
            .firstOrNull()

    return ann?.let {
        val defaults = AnnotationFactory.build(annotation)
        Proxy.newProxyInstance(javaClass.getClassLoader(), arrayOf(annotation)) {
            proxy,
            method,
            args ->
            val attr = ann.elements().firstOrNull { it.name().stringValue() == method.name }
            attr?.value()?.let { value ->
                when (method.returnType) {
                    String::class.java -> (value as OfString).stringValue()
                    Boolean::class.java -> (value as OfBoolean).booleanValue()
                    Int::class.java -> (value as OfInteger).intValue()
                    Long::class.java -> (value as OfLong).longValue()
                    Float::class.java -> (value as OfFloat).floatValue()
                    Double::class.java -> (value as OfDouble).doubleValue()
                    Char::class.java -> (value as OfCharacter).charValue()
                    Byte::class.java -> (value as OfByte).byteValue()
                    else -> TODO("Unsupported method: ${method.returnType}")
                }
            } ?: method.invoke(defaults)
        }
    } as A?
}
