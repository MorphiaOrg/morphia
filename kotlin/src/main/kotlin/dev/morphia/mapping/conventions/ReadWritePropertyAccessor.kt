package dev.morphia.mapping.conventions

import org.bson.codecs.pojo.PropertyAccessor
import kotlin.reflect.KMutableProperty
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter

class ReadWritePropertyAccessor(private val kProperty: KMutableProperty<*>) : PropertyAccessor<Any> {
    init {
        kProperty.javaField?.isAccessible = true
        kProperty.javaGetter?.isAccessible = true
        kProperty.javaSetter?.isAccessible = true
    }

    override fun <S : Any?> get(instance: S): Any? {
        val getter = kProperty.getter
        val value = getter.call(instance)
        return value
    }

    override fun <S : Any?> set(instance: S, value: Any?) {
        kProperty.setter.call(instance, value)
    }
}
