package dev.morphia.critter.parser.asm

import dev.morphia.config.MorphiaConfig
import dev.morphia.mapping.conventions.MorphiaDefaultsConvention
import org.objectweb.asm.Type

object Generators {
    var config = MorphiaConfig.load()
    var convention = MorphiaDefaultsConvention()

    fun wrap(fieldType: Type): Type {
        return when (fieldType) {
            Type.VOID_TYPE -> Type.getType(Void::class.java)
            Type.BOOLEAN_TYPE -> Type.getType(Boolean::class.java)
            Type.CHAR_TYPE -> Type.getType(Char::class.java)
            Type.BYTE_TYPE -> Type.getType(Byte::class.java)
            Type.SHORT_TYPE -> Type.getType(Short::class.java)
            Type.INT_TYPE -> Type.getType(Integer::class.java)
            Type.FLOAT_TYPE -> Type.getType(Float::class.java)
            Type.LONG_TYPE -> Type.getType(Long::class.java)
            Type.DOUBLE_TYPE -> Type.getType(Double::class.java)
            else -> fieldType
        }
    }
}
