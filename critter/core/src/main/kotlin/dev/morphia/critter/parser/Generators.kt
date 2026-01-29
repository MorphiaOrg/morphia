package dev.morphia.critter.parser

import dev.morphia.config.MorphiaConfig
import dev.morphia.config.MorphiaConfigHelper.MORPHIA_CONFIG_PROPERTIES
import dev.morphia.mapping.Mapper
import dev.morphia.mapping.conventions.MorphiaDefaultsConvention
import org.objectweb.asm.Type
import org.objectweb.asm.Type.ARRAY

object Generators {
    var configFile = MORPHIA_CONFIG_PROPERTIES

    val config: MorphiaConfig by lazy { MorphiaConfig.load(configFile) }

    val mapper: Mapper by lazy { Mapper(config) }

    var convention = MorphiaDefaultsConvention()

    fun wrap(fieldType: Type): Type {
        return when (fieldType) {
            Type.VOID_TYPE -> Type.getType(Void::class.java)
            Type.BOOLEAN_TYPE -> Type.getType(Boolean::class.java)
            Type.CHAR_TYPE -> Type.getType(Char::class.java)
            Type.BYTE_TYPE -> Type.getType(Byte::class.java)
            Type.SHORT_TYPE -> Type.getType(Short::class.java)
            Type.INT_TYPE -> Type.getType(Int::class.java)
            Type.FLOAT_TYPE -> Type.getType(Float::class.java)
            Type.LONG_TYPE -> Type.getType(Long::class.java)
            Type.DOUBLE_TYPE -> Type.getType(Double::class.java)
            else -> fieldType
        }
    }

    fun Type.isArray(): Boolean = sort == ARRAY

    fun Type.asClass(
        classLoader: ClassLoader = Thread.currentThread().contextClassLoader
    ): Class<*> =
        when (this) {
            Type.VOID_TYPE -> Void.TYPE
            Type.BOOLEAN_TYPE -> Boolean::class.java
            Type.CHAR_TYPE -> Char::class.java
            Type.BYTE_TYPE -> Byte::class.java
            Type.SHORT_TYPE -> Short::class.java
            Type.INT_TYPE -> Int::class.java
            Type.FLOAT_TYPE -> Float::class.java
            Type.LONG_TYPE -> Long::class.java
            Type.DOUBLE_TYPE -> Double::class.java
            else ->
                Class.forName(
                    if (sort == ARRAY) descriptor.replace('/', '.') else className,
                    false,
                    classLoader,
                )
        }
}
