package dev.morphia.mapping;

import dev.morphia.Datastore;
import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Handler;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Transient;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.codec.ArrayFieldAccessor;
import dev.morphia.mapping.codec.FieldAccessor;
import dev.morphia.mapping.codec.MorphiaPropertySerialization;
import dev.morphia.mapping.codec.pojo.FieldModelBuilder;
import dev.morphia.mapping.codec.pojo.MorphiaModelBuilder;
import org.bson.codecs.pojo.ClassModelBuilder;
import org.bson.codecs.pojo.Convention;
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
import java.util.ListIterator;
import java.util.stream.Collectors;

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isStatic;
import static org.bson.codecs.pojo.PojoBuilderHelper.createPropertyModelBuilder;

/**
 * A set of conventions to apply to Morphia entities
 */
@SuppressWarnings("unchecked")
public class MorphiaConvention implements Convention {
    private Datastore datastore;
    private MapperOptions options;

    MorphiaConvention(final Datastore datastore, final MapperOptions options) {
        this.datastore = datastore;
        this.options = options;
    }

    @Override
    public void apply(final ClassModelBuilder<?> classModelBuilder) {
        if (!(classModelBuilder instanceof MorphiaModelBuilder)) {
            return;
        }
        MorphiaModelBuilder modelBuilder = (MorphiaModelBuilder) classModelBuilder;
        final InstanceCreatorFactoryImpl creatorFactory = new InstanceCreatorFactoryImpl(modelBuilder.getType());
        modelBuilder.instanceCreatorFactory(creatorFactory);
        modelBuilder.discriminator(modelBuilder.getType().getName())
                    .discriminatorKey(options.getDiscriminatorField());

        final Entity entity = getAnnotation(modelBuilder, Entity.class);
        final Embedded embedded = getAnnotation(modelBuilder, Embedded.class);
        if (entity != null) {
            modelBuilder.enableDiscriminator(entity.useDiscriminator());
        } else if (embedded != null) {
            modelBuilder.enableDiscriminator(embedded.useDiscriminator());
        } else {
            modelBuilder.enableDiscriminator(true);
        }

        final List<String> properties = classModelBuilder.getPropertyModelBuilders().stream()
                                                         .map(PropertyModelBuilder::getName)
                                                         .collect(Collectors.toList());
        for (final String name : properties) {
            classModelBuilder.removeProperty(name);
        }
        classModelBuilder.propertyNameToTypeParameterMap(Collections.emptyMap());

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

                List<String> names = new ArrayList<>(List.of(getMappedFieldName(builder)));
                AlsoLoad alsoLoad = builder.getAnnotation(AlsoLoad.class);
                if (alsoLoad != null) {
                    names.addAll(Arrays.asList(alsoLoad.value()));
                }
                for (final String name : names) {
                    buildProperty(modelBuilder, builder, field, name);
                }
            }
        }

    }

    private <T extends Annotation> T getAnnotation(final ClassModelBuilder<?> classModelBuilder, final Class<T> klass) {
        final List<Annotation> annotations = classModelBuilder.getAnnotations();
        if (!annotations.isEmpty()) {
            final ListIterator<Annotation> iterator = annotations.listIterator(annotations.size());
            while (iterator.hasPrevious()) {
                final Annotation annotation = iterator.previous();
                if (klass.equals(annotation.annotationType())) {
                    return klass.cast(annotation);
                }
            }
        }

        return null;
    }

    private static boolean isTransient(final FieldModelBuilder<?> field) {
        return field.hasAnnotation(Transient.class)
               || field.hasAnnotation(java.beans.Transient.class)
               || Modifier.isTransient(field.getField().getModifiers());
    }

    private static String getMappedFieldName(final FieldModelBuilder<?> field) {
        if (field.hasAnnotation(Id.class)) {
            return "_id";
        } else if (field.hasAnnotation(Property.class)) {
            final Property mv = field.getAnnotation(Property.class);
            if (!mv.value().equals(Mapper.IGNORED_FIELDNAME)) {
                return mv.value();
            }
        } else if (field.hasAnnotation(Reference.class)) {
            final Reference mr = field.getAnnotation(Reference.class);
            if (!mr.value().equals(Mapper.IGNORED_FIELDNAME)) {
                return mr.value();
            }
        } else if (field.hasAnnotation(Embedded.class)) {
            final Embedded me = field.getAnnotation(Embedded.class);
            if (!me.value().equals(Mapper.IGNORED_FIELDNAME)) {
                return me.value();
            }
        } else if (field.hasAnnotation(Version.class)) {
            final Version me = field.getAnnotation(Version.class);
            if (!me.value().equals(Mapper.IGNORED_FIELDNAME)) {
                return me.value();
            }
        }

        return field.getName();
    }

    private void buildProperty(final MorphiaModelBuilder modelBuilder,
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
        configureCodec(property, field);

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

    private void configureCodec(final PropertyModelBuilder builder, final Field field) {
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

}
