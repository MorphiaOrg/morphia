package dev.morphia.critter.conventions

import dev.morphia.annotations.Id
import dev.morphia.annotations.Property
import dev.morphia.annotations.Reference
import dev.morphia.annotations.Transient
import dev.morphia.annotations.Version
import dev.morphia.annotations.internal.MorphiaInternal
import dev.morphia.config.MorphiaConfig
import dev.morphia.mapping.Mapper

/**
 * @since 3.0
 * @hidden @morphia.internal
 */
@MorphiaInternal
object PropertyConvention {
    fun transientAnnotations(): List<Class<out Annotation>> {
        return listOf(
            Transient::class.java,
            kotlin.jvm.Transient::class.java,
            java.beans.Transient::class.java
        )
    }

    @MorphiaInternal
    fun mappedName(
        config: MorphiaConfig,
        annotations: Map<String, Annotation>,
        modelName: String
    ): String {
        val property = annotations.get(Property::class.java.getName()) as Property?
        val reference = annotations.get(Reference::class.java.getName()) as Reference?
        val version = annotations.get(Version::class.java.getName()) as Version?
        val id = annotations.get(Id::class.java.getName()) as Id?
        return when {
            id != null -> "_id"
            property != null && property.value != Mapper.IGNORED_FIELDNAME -> property.value
            reference != null && reference.value != Mapper.IGNORED_FIELDNAME -> reference.value
            version != null && version.value != Mapper.IGNORED_FIELDNAME -> version.value
            else -> config.propertyNaming().apply(modelName)
        }
    }
}
