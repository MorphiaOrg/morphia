package dev.morphia.mapping.conventions.kotlin

import dev.morphia.Datastore
import dev.morphia.mapping.MapperOptions.Builder
import dev.morphia.mapping.codec.pojo.EntityModelBuilder
import dev.morphia.mapping.conventions.MorphiaConvention
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.bson.codecs.pojo.PropertyAccessor
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter

class KotlinPropertyDiscovery(private val optionsBuilder: Builder) : MorphiaConvention {
    @Suppress("UNCHECKED_CAST")
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    override fun apply(datastore: Datastore, builder: EntityModelBuilder) {
        val field = builder.type.declaredFields.firstOrNull { it.name == "\$\$delegatedProperties" }
        if (field != null) {
            field.trySetAccessible()
            for (kProperty in field.get(builder.type) as Array<KProperty<*>>) {
                builder.propertyModelByName("${kProperty.name}\$delegate")
                    .name(kProperty.name)
                    .discoverMappedName(optionsBuilder.build())
                    .accessor(ReadWritePropertyAccessor(kProperty as KMutableProperty<*>))
            }
        }
    }
}

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
