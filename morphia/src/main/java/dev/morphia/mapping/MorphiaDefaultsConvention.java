package dev.morphia.mapping;

import dev.morphia.Datastore;
import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Handler;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Transient;
import dev.morphia.annotations.experimental.IdField;
import dev.morphia.mapping.codec.ArrayFieldAccessor;
import dev.morphia.mapping.codec.FieldAccessor;
import dev.morphia.mapping.codec.MorphiaPropertySerialization;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.pojo.FieldModelBuilder;
import morphia.org.bson.codecs.pojo.TypeData;
import org.bson.codecs.pojo.PropertyAccessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isStatic;

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

        processFields(modelBuilder, datastore, options);

        if (modelBuilder.idFieldName() == null) {
            IdField idField = modelBuilder.getAnnotation(IdField.class);
            if (idField != null) {
                modelBuilder.idFieldName(idField.value());
                FieldModelBuilder<?> fieldModelBuilder = modelBuilder.fieldModelByFieldName(idField.value());
                fieldModelBuilder.mappedName("_id");
            }
        }

    }

    @SuppressWarnings("rawtypes")
    void processFields(final EntityModelBuilder<?> modelBuilder, final Datastore datastore, final MapperOptions options) {
        Iterator<FieldModelBuilder<?>> iterator = modelBuilder.fieldModels().iterator();
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

                AlsoLoad alsoLoad = builder.getAnnotation(AlsoLoad.class);
                if (alsoLoad != null) {
                    for (final String name : alsoLoad.value()) {
                        builder.alternateName(name);
                    }
                }
            }

            buildField(datastore, options, builder, field);
        }
    }

    private void buildField(final Datastore datastore,
                            final MapperOptions options,
                            final FieldModelBuilder<?> builder,
                            final Field field) {

        builder
            .serialization(new MorphiaPropertySerialization(options, builder))
            .accessor(getAccessor(field, builder));
        configureCodec(datastore, builder, field);

        if (isNotConcrete(builder.getTypeData())) {
            builder.discriminatorEnabled(true);
        }
    }

    private PropertyAccessor<?> getAccessor(final Field field, final FieldModelBuilder<?> property) {
        return field.getType().isArray() && !field.getType().getComponentType().equals(byte.class)
               ? new ArrayFieldAccessor(property.getTypeData(), field)
               : new FieldAccessor(field);
    }

    private void configureCodec(final Datastore datastore, final FieldModelBuilder<?> builder, final Field field) {
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
        Class<?> type;
        if (!typeData.getTypeParameters().isEmpty()) {
            type = typeData.getTypeParameters().get(typeData.getTypeParameters().size() - 1).getType();
        } else {
            type = typeData.getType();
        }

        return isNotConcrete(type);
    }

    private Handler getHandler(final FieldModelBuilder<?> builder) {
        Handler handler = builder.getTypeData().getType().getAnnotation(Handler.class);

        if (handler == null) {
            handler = (Handler) builder.getAnnotations()
                                    .stream().filter(a -> a.getClass().equals(Handler.class))
                                    .findFirst().orElse(null);
            if (handler == null) {
                for (Annotation annotation : builder.getAnnotations()) {
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
