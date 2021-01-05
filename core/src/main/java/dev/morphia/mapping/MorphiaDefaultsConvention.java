package dev.morphia.mapping;

import static java.lang.reflect.Modifier.*;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.*;
import java.util.*;

import dev.morphia.*;
import dev.morphia.annotations.*;
import dev.morphia.annotations.experimental.*;
import dev.morphia.mapping.codec.*;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.codec.pojo.*;
import org.bson.codecs.pojo.*;

/**
 * A set of conventions to apply to Morphia entities
 */
@SuppressWarnings("unchecked")
public class MorphiaDefaultsConvention implements MorphiaConvention {

    private static boolean isTransient(FieldModelBuilder field) {
        return field.hasAnnotation(Transient.class)
               || field.hasAnnotation(java.beans.Transient.class)
               || Modifier.isTransient(field.field().getModifiers());
    }

    @Override
    public void apply(Datastore datastore, EntityModelBuilder modelBuilder) {
        MapperOptions options = datastore.getMapper().getOptions();

        final Entity entity = modelBuilder.getAnnotation(Entity.class);
        final Embedded embedded = modelBuilder.getAnnotation(Embedded.class);
        if (entity != null) {
            modelBuilder.enableDiscriminator(entity.useDiscriminator());
            modelBuilder.discriminatorKey(applyDefaults(entity.discriminatorKey(), options.getDiscriminatorKey()));
        } else {
            modelBuilder.enableDiscriminator(embedded == null || embedded.useDiscriminator());
            modelBuilder.discriminatorKey(applyDefaults(embedded != null ? embedded.discriminatorKey() : Mapper.IGNORED_FIELDNAME,
                options.getDiscriminatorKey()));
        }

        options.getDiscriminator().apply(modelBuilder);

        processFields(modelBuilder, datastore, options);

        if (modelBuilder.idFieldName() == null) {
            IdField idField = modelBuilder.getAnnotation(IdField.class);
            if (idField != null) {
                modelBuilder.idFieldName(idField.value());
                FieldModelBuilder fieldModelBuilder = modelBuilder.fieldModelByFieldName(idField.value());
                fieldModelBuilder.mappedName("_id");
            }
        }

    }

    @SuppressWarnings("rawtypes")
    void processFields(EntityModelBuilder modelBuilder, Datastore datastore, MapperOptions options) {
        Iterator<FieldModelBuilder> iterator = modelBuilder.fieldModels().iterator();
        while (iterator.hasNext()) {
            final FieldModelBuilder builder = iterator.next();
            final Field field = builder.field();

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
                    for (String name : alsoLoad.value()) {
                        builder.alternateName(name);
                    }
                }
            }

            buildField(datastore, options, builder, field);
        }
    }

    private void buildField(Datastore datastore,
                            MapperOptions options,
                            FieldModelBuilder builder,
                            Field field) {

        builder
            .serialization(new MorphiaPropertySerialization(options, builder))
            .accessor(getAccessor(field, builder));
        configureCodec(datastore, builder, field);

        if (isNotConcrete(builder.typeData())) {
            builder.discriminatorEnabled(true);
        }
    }

    private PropertyAccessor<? super Object> getAccessor(Field field, FieldModelBuilder property) {
        return field.getType().isArray() && !field.getType().getComponentType().equals(byte.class)
               ? new ArrayFieldAccessor(property.typeData(), field)
               : new FieldAccessor(field);
    }

    private void configureCodec(Datastore datastore, FieldModelBuilder builder, Field field) {
        Handler handler = getHandler(builder);
        if (handler != null) {
            try {
                builder.codec(handler.value()
                                     .getDeclaredConstructor(Datastore.class, Field.class, TypeData.class)
                                     .newInstance(datastore, field, builder.typeData()));
            } catch (ReflectiveOperationException e) {
                throw new MappingException(e.getMessage(), e);
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

    private Handler getHandler(FieldModelBuilder builder) {
        Handler handler = builder.typeData().getType().getAnnotation(Handler.class);

        if (handler == null) {
            handler = (Handler) builder.annotations()
                                    .stream().filter(a -> a.getClass().equals(Handler.class))
                                    .findFirst().orElse(null);
            if (handler == null) {
                for (Annotation annotation : builder.annotations()) {
                    handler = annotation.annotationType().getAnnotation(Handler.class);
                    if (handler != null) {
                        break;
                    }
                }
            }
        }

        return handler;
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
