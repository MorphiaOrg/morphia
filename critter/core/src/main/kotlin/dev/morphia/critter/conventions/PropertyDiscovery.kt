package dev.morphia.critter.conventions

import com.mongodb.lang.NonNull
import com.mongodb.lang.Nullable
import dev.morphia.annotations.Id
import dev.morphia.annotations.Property
import dev.morphia.annotations.Reference
import dev.morphia.annotations.Transient
import dev.morphia.annotations.Version
import dev.morphia.annotations.internal.MorphiaInternal
import dev.morphia.mapping.Mapper
import dev.morphia.mapping.MappingException
import dev.morphia.mapping.codec.ArrayFieldAccessor
import dev.morphia.mapping.codec.FieldAccessor
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.PropertyModel
import dev.morphia.mapping.codec.pojo.TypeData
import dev.morphia.sofia.Sofia
import io.github.dmlloyd.classfile.ClassBuilder
import io.github.dmlloyd.classfile.ClassModel
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import org.bson.codecs.pojo.PropertyAccessor

@MorphiaInternal
class PropertyDiscovery : CritterConvention {
    override fun apply(mapper: Mapper, builder: ClassBuilder, model: ClassModel) {
        TODO()
        /*
                if (model.properties.isEmpty()) {
                    val list: MutableSet<Class<*>> = LinkedHashSet()
                    list.add(model.type)
                    list.addAll(model.classHierarchy())

                    for (type in list) {
                        for (field in type.declaredFields) {
                            if (!Modifier.isStatic(field.modifiers) && !isTransient(field)) {
                                val typeData = model.getTypeData(type, TypeData.get(field), field.genericType)
                                val externalEntity = type.getAnnotation(
                                    ExternalEntity::class.java
                                )
                                val target: Class<*>? = externalEntity?.target
                                val propertyModel = PropertyModel(model)
                                model.addProperty(
                                    propertyModel
                                        .name(field.name)
                                        .typeData(typeData)
                                        .annotations(List.of(*field.declaredAnnotations))
                                        .isFinal(Modifier.isFinal(field.modifiers))
                                        .accessor(getAccessor(getTargetField(model, target, field), typeData))
                                        .mappedName(discoverMappedName(mapper, propertyModel))
                                )
                            }
                        }
                    }
                }
        */
    }

    @NonNull
    private fun getTargetField(
        model: EntityModel,
        @Nullable target: Class<*>?,
        @NonNull field: Field
    ): Field {
        try {
            return if (target == null) field else target.getDeclaredField(field.name)
        } catch (e: NoSuchFieldException) {
            throw MappingException(
                Sofia.mismatchedFieldOnExternalType(field.name, model.type.name, model.type.name)
            )
        }
    }

    private fun getAccessor(field: Field, typeData: TypeData<*>): PropertyAccessor<in Any?> {
        return if (field.type.isArray && field.type.componentType != Byte::class.javaPrimitiveType)
            ArrayFieldAccessor(typeData, field)
        else FieldAccessor(field)
    }

    companion object {
        fun isTransient(field: Field): Boolean {
            return field.getDeclaredAnnotation(Transient::class.java) != null ||
                field.getDeclaredAnnotation(java.beans.Transient::class.java) != null ||
                Modifier.isTransient(field.modifiers)
        }

        @MorphiaInternal
        fun discoverMappedName(mapper: Mapper, model: PropertyModel): String {
            val config = mapper.config
            val property = model.getAnnotation(Property::class.java)
            val reference = model.getAnnotation(Reference::class.java)
            val version = model.getAnnotation(Version::class.java)
            val mappedName =
                if (model.hasAnnotation(Id::class.java)) {
                    "_id"
                } else if (property != null && property.value != Mapper.IGNORED_FIELDNAME) {
                    property.value
                } else if (reference != null && reference.value != Mapper.IGNORED_FIELDNAME) {
                    reference.value
                } else if (version != null && version.value != Mapper.IGNORED_FIELDNAME) {
                    version.value
                } else {
                    config.propertyNaming().apply(model.name)
                }
            return mappedName
        }
    }
}
