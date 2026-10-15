package dev.morphia.mapping.conventions

import dev.morphia.mapping.Mapper
import dev.morphia.mapping.codec.pojo.EntityModel
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

class KotlinDelegatedPropertiesDiscovery : MorphiaConvention {
    @Suppress("UNCHECKED_CAST")
    override fun apply(mapper: Mapper, model: EntityModel) {
        model.type.declaredFields
            .firstOrNull { it.name == "\$\$delegatedProperties" }
            ?.let { field ->
                field.trySetAccessible()
                for (kProperty in field.get(model.type) as Array<KProperty<*>>) {
                    val property = model.getProperty("${kProperty.name}\$delegate") ?: continue
                    property.accessor(ReadWritePropertyAccessor(kProperty as KMutableProperty<*>))
                    model.renameProperty(
                        property,
                        kProperty.name,
                        FieldDiscovery.discoverMappedName(mapper, property, kProperty.name),
                    )
                }
            }
    }
}
