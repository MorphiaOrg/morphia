package dev.morphia.mapping;

import dev.morphia.Datastore;
import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Handler;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Transient;
import dev.morphia.annotations.experimental.IdField;
import dev.morphia.mapping.codec.ArrayFieldAccessor;
import dev.morphia.mapping.codec.FieldAccessor;
import dev.morphia.mapping.codec.MorphiaPropertySerialization;
import dev.morphia.mapping.codec.pojo.FieldModelBuilder;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import org.bson.codecs.pojo.PropertyAccessor;
import org.bson.codecs.pojo.PropertyMetadata;
import org.bson.codecs.pojo.PropertyModelBuilder;
import org.bson.codecs.pojo.TypeData;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isStatic;
import static org.bson.codecs.pojo.PojoBuilderHelper.createPropertyModelBuilder;

/**
 * A set of conventions to apply to Morphia entities
 */
@SuppressWarnings("unchecked")
public class MorphiaDefaultsConvention implements MorphiaConvention {

    private static boolean isTransient(final FieldModelBuilder<?> field) {
        return field.hasAnnotation(Transient.class)
               || field.hasAnnotation(java.beans.Transient.class)
               || Modifier.isTransient(field.getField().getModifiers());
    }

    @Override
    public void apply(final Datastore datastore, final EntityModelBuilder<?> modelBuilder) {
        MapperOptions options = datastore.getMapper().getOptions();

        final Entity entity = modelBuilder.getAnnotation(Entity.class);
        final Embedded embedded = modelBuilder.getAnnotation(Embedded.class);
        if (entity != null) {
            modelBuilder.enableDiscriminator(entity.useDiscriminator());
            modelBuilder.discriminatorKey(applyDefaults(entity.discriminatorKey(), options.getDiscriminatorKey()));
        } else if (embedded != null) {
            modelBuilder.enableDiscriminator(embedded.useDiscriminator());
            modelBuilder.discriminatorKey(applyDefaults(embedded.discriminatorKey(), options.getDiscriminatorKey()));
        } else {
            modelBuilder.enableDiscriminator(true);
            throw new UnsupportedOperationException("Types should either have @Entity or @Embedded.  Should 'never' get here.");
        }

        options.getDiscriminator().apply(modelBuilder);

        final List<String> properties = modelBuilder.getPropertyModelBuilders().stream()
                                                         .map(PropertyModelBuilder::getName)
                                                         .collect(Collectors.toList());
        for (final String name : properties) {
            modelBuilder.removeProperty(name);
        }
        modelBuilder.propertyNameToTypeParameterMap(Collections.emptyMap());

        processFields(datastore, modelBuilder, options);

        if (modelBuilder.getIdPropertyName() == null) {
            IdField idField = modelBuilder.getAnnotation(IdField.class);
            if (idField != null) {
                modelBuilder.idPropertyName(idField.value());
                FieldModelBuilder<?> fieldModelBuilder = modelBuilder.getFieldModelBuilders().stream()
                                                                     .filter(builder -> builder.getName().equals(idField.value()))
                                                                     .findFirst().orElseThrow();
                fieldModelBuilder.mappedName("_id");
            }
        }

    }

    void processFields(final Datastore datastore, final EntityModelBuilder<?> modelBuilder, final MapperOptions options) {
        Iterator<FieldModelBuilder<?>> iterator = modelBuilder.getFieldModelBuilders().iterator();
        while (iterator.hasNext()) {
            final FieldModelBuilder<?> builder = iterator.next();
            final Field field = builder.getField();

            if (isStatic(field.getModifiers()) || isTransient(builder)) {
                iterator.remove();
            } else {
                Property property = builder.getAnnotation(Property.class);
                if (property != null && !property.concreteClass().equals(Object.class)) {
                    TypeData typeData = TypeData.newInstance(field.getGenericType(), property.concreteClass());
                    builder.typeData(typeData);
                }

                List<String> names = new ArrayList<>(List.of(builder.mappedName()));
                AlsoLoad alsoLoad = builder.getAnnotation(AlsoLoad.class);
                if (alsoLoad != null) {
                    names.addAll(Arrays.asList(alsoLoad.value()));
                }
                for (final String name : names) {
                    buildProperty(datastore, options, modelBuilder, builder, field, name);
                }
            }
        }
    }

    private void buildProperty(final Datastore datastore, final MapperOptions options, final EntityModelBuilder modelBuilder,
                               final FieldModelBuilder<?> builder, final Field field, final String mappedName) {
        final PropertyMetadata<?> propertyMetadata = new PropertyMetadata<>(builder.getName(),
            modelBuilder.getType().getName(), builder.getTypeData())
                                                         .field(field);
        final PropertyModelBuilder<?> property = createPropertyModelBuilder(propertyMetadata);
        modelBuilder.addProperty(property);

        property.typeData((TypeData) builder.getTypeData());

        if (builder.hasAnnotation(Id.class)) {
            modelBuilder.idPropertyName(property.getReadName());
        }

        property.readName(mappedName)
                .writeName(mappedName)
                .propertySerialization(new MorphiaPropertySerialization(options, builder))
                .readAnnotations(builder.getAnnotations())
                .writeAnnotations(builder.getAnnotations())
                .propertyAccessor(getPropertyAccessor(field, property));
        configureCodec(datastore, property, field);

        if (isNotConcrete(property.getTypeData())) {
            property.discriminatorEnabled(true);
        }
    }

    private PropertyAccessor getPropertyAccessor(final Field field,
                                                 final PropertyModelBuilder<?> property) {

        return field.getType().isArray() && !field.getType().getComponentType().equals(byte.class)
               ? new ArrayFieldAccessor(property.getTypeData(), field)
               : new FieldAccessor(field);
    }

    private void configureCodec(final Datastore datastore, final PropertyModelBuilder builder, final Field field) {
        Handler handler = getHandler(builder);
        if (handler != null) {
            try {
                builder.codec(handler.value()
                                     .getDeclaredConstructor(Datastore.class, Field.class, TypeData.class)
                                     .newInstance(datastore, field, builder.getTypeData()));
            } catch (ReflectiveOperationException e) {
                throw new MappingException(e.getMessage(), e);
            }
        }
    }

    private boolean isNotConcrete(final TypeData<?> typeData) {
        Class type;
        if (!typeData.getTypeParameters().isEmpty()) {
            type = typeData.getTypeParameters().get(typeData.getTypeParameters().size() - 1).getType();
        } else {
            type = typeData.getType();
        }

        return isNotConcrete(type);
    }

    private Handler getHandler(final PropertyModelBuilder builder) {
        Handler handler = (Handler) builder.getTypeData().getType().getAnnotation(Handler.class);

        if (handler == null) {
            final List<Annotation> readAnnotations = builder.getReadAnnotations();
            handler = (Handler) readAnnotations
                                    .stream().filter(a -> a.getClass().equals(Handler.class))
                                    .findFirst().orElse(null);
            if (handler == null) {
                for (Annotation annotation : readAnnotations) {
                    handler = annotation.annotationType().getAnnotation(Handler.class);
                }
            }
        }

        return handler;
    }

    private boolean isNotConcrete(final Class type) {
        Class componentType = type;
        if (type.isArray()) {
            componentType = type.getComponentType();
        }
        return componentType.isInterface() || isAbstract(componentType.getModifiers());
    }

    String applyDefaults(final String configured, final String defaultValue) {
        if (!configured.equals(Mapper.IGNORED_FIELDNAME)) {
            return configured;
        } else {
            return defaultValue;
        }
    }

}
