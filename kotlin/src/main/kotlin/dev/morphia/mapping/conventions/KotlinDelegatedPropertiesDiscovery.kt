package dev.morphia.mapping.conventions

import dev.morphia.Datastore
import dev.morphia.mapping.codec.pojo.EntityModelBuilder
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

class KotlinDelegatedPropertiesDiscovery : MorphiaConvention {
    @Suppress("UNCHECKED_CAST")
    override fun apply(datastore: Datastore, builder: EntityModelBuilder) {
        val field = builder.type().declaredFields.firstOrNull { it.name == "\$\$delegatedProperties" }
        if (field != null) {
            field.trySetAccessible()
            for (kProperty in field.get(builder.type()) as Array<KProperty<*>>) {
                builder.propertyModelByName("${kProperty.name}\$delegate")
                    .name(kProperty.name)
                    .discoverMappedName()
                    .accessor(ReadWritePropertyAccessor(kProperty as KMutableProperty<*>))
            }
        }
    }
}

