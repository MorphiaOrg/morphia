package dev.morphia.mapping.conventions;

import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Transient;
import dev.morphia.annotations.Version;
import dev.morphia.annotations.experimental.IdField;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.codec.MorphiaPropertySerialization;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.pojo.PropertyModelBuilder;
import dev.morphia.mapping.codec.pojo.TypeData;

import java.lang.reflect.Modifier;
import java.util.Iterator;

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isStatic;

/**
 * A set of conventions to apply to Morphia entities
 *
 * @since 2.2
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
public class ConfigureProperties implements MorphiaConvention {

    private static boolean isTransient(PropertyModelBuilder property) {
        return property.hasAnnotation(Transient.class)
               || property.hasAnnotation(java.beans.Transient.class)
               || Modifier.isTransient(property.modifiers());
    }

    @Override
    public void apply(Mapper mapper, EntityModelBuilder modelBuilder) {
        MapperOptions options = mapper.getOptions();

        processProperties(modelBuilder, options);

        if (modelBuilder.idPropertyName() == null) {
            IdField idProperty = modelBuilder.getAnnotation(IdField.class);
            if (idProperty != null) {
                modelBuilder.idPropertyName(idProperty.value());
                PropertyModelBuilder propertyModelBuilder = modelBuilder.propertyModelByName(idProperty.value());
                propertyModelBuilder.mappedName("_id");
            }
        }

    }

    private void buildProperty(MapperOptions options, PropertyModelBuilder builder) {

        builder.serialization(new MorphiaPropertySerialization(options, builder));
        if (isNotConcrete(builder.typeData())) {
            builder.discriminatorEnabled(true);
        }
    }

    @SuppressWarnings("rawtypes")
    void processProperties(EntityModelBuilder modelBuilder, MapperOptions options) {
        Iterator<PropertyModelBuilder> iterator = modelBuilder.propertyModels().iterator();
        while (iterator.hasNext()) {
            final PropertyModelBuilder builder = iterator.next();
            final int modifiers = builder.modifiers();

            if (isStatic(modifiers) || isTransient(builder)) {
                iterator.remove();
            } else {
                Property property = builder.getAnnotation(Property.class);
                if (property != null && !property.concreteClass().equals(Object.class)) {
                    TypeData typeData = builder.typeData().withType(property.concreteClass());
                    builder.typeData(typeData);
                }

                AlsoLoad alsoLoad = builder.getAnnotation(AlsoLoad.class);
                if (alsoLoad != null) {
                    for (String name : alsoLoad.value()) {
                        builder.alternateName(name);
                    }
                }

                if (builder.getAnnotation(Id.class) != null) {
                    modelBuilder.idPropertyName(builder.name());
                }
                if (builder.getAnnotation(Version.class) != null) {
                    modelBuilder.versionPropertyName(builder.name());
                }

                buildProperty(options, builder);
            }
        }
    }

    private boolean isNotConcrete(TypeData<?> typeData) {
        Class<?> type;
        if (!typeData.getTypeParameters().isEmpty()) {
            type = typeData.getTypeParameters().get(typeData.getTypeParameters().size() - 1).getType();
        } else {
            type = typeData.getType();
        }

        return isNotConcrete(type);
    }


    private boolean isNotConcrete(Class<?> type) {
        Class<?> componentType = type;
        if (type.isArray()) {
            componentType = type.getComponentType();
        }
        return componentType.isInterface() || isAbstract(componentType.getModifiers());
    }

    String applyDefaults(String configured, String defaultValue) {
        if (!configured.equals(Mapper.IGNORED_FIELDNAME)) {
            return configured;
        } else {
            return defaultValue;
        }
    }

}
