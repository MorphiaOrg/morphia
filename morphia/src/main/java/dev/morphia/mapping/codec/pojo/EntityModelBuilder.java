package dev.morphia.mapping.codec.pojo;

import dev.morphia.Datastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MorphiaConvention;
import dev.morphia.sofia.Sofia;
import morphia.org.bson.codecs.pojo.TypeData;
import morphia.org.bson.codecs.pojo.TypeParameterMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import static morphia.org.bson.codecs.pojo.PojoSpecializationHelper.specializeTypeData;

/**
 * Builder for EntityModels
 *
 * @param <T> the entity type
 * @morphia.internal
 * @since 2.0
 */
@SuppressWarnings("UnusedReturnValue")
public class EntityModelBuilder<T> {
    private final Datastore datastore;
    private final List<FieldModelBuilder<?>> fieldModels = new ArrayList<>();
    private final Class<T> type;
    private final Map<Class<? extends Annotation>, Annotation> annotationsMap = new HashMap<>();
    private boolean discriminatorEnabled;
    private String discriminator;
    private String discriminatorKey;
    private String idFieldName;
    private final Set<Class<?>> classes = new LinkedHashSet<>();
    private final Set<Class<?>> interfaces = new LinkedHashSet<>();
    private final Set<Annotation> annotations = new LinkedHashSet<>();

    /**
     * Create a builder
     *
     * @param datastore the datastore to use
     * @param type      the entity type
     */
    public EntityModelBuilder(final Datastore datastore, final Class<T> type) {
        this.datastore = datastore;
        this.type = type;

        TypeData<?> parentClassTypeData = null;
        buildHierarchy(this.type);
        Map<String, TypeParameterMap> propertyTypeParameterMap = new HashMap<>();

        for (Class<?> klass : classes) {
            processFields(klass, parentClassTypeData, processTypeNames(klass), propertyTypeParameterMap);

            parentClassTypeData = TypeData.newInstance(klass.getGenericSuperclass(), klass);
        }
    }

    /**
     * Adds a field to the model
     *
     * @param builder the field to add
     */
    public void addModel(final FieldModelBuilder<?> builder) {
        fieldModels.add(builder);
    }

    /**
     * @return the annotation on this model
     */
    public Set<Annotation> annotations() {
        return annotations;
    }

    /**
     * Creates a new ClassModel instance based on the mapping data provided.
     *
     * @return the new instance
     */
    public EntityModel<T> build() {
        annotations.forEach(a -> annotationsMap.putIfAbsent(a.annotationType(), a));

        for (MorphiaConvention convention : datastore.getMapper().getOptions().getConventions()) {
            convention.apply(datastore, this);
        }

        if (discriminatorEnabled) {
            Objects.requireNonNull(discriminatorKey, Sofia.notNull("discriminatorKey"));
            Objects.requireNonNull(discriminator, Sofia.notNull("discriminator"));
        }

        return new EntityModel<>(this);
    }

    /**
     * Sets the discriminator
     *
     * @param discriminator the discriminator
     * @return this
     */
    public EntityModelBuilder<T> discriminator(final String discriminator) {
        this.discriminator = discriminator;
        return this;
    }

    /**
     * @return the discriminator
     */
    public String discriminator() {
        return discriminator;
    }

    /**
     * Sets the mapped key name to use when storing the discriminator value
     *
     * @param key the key to use
     * @return this
     */
    public EntityModelBuilder<T> discriminatorKey(final String key) {
        discriminatorKey = key;
        return this;
    }

    /**
     * @return the discriminator key
     */
    public String discriminatorKey() {
        return discriminatorKey;
    }

    /**
     * Enables or disables the use of a discriminator when serializing
     *
     * @param enabled true to enable the use of a discriminator
     * @return this
     */
    public EntityModelBuilder<T> enableDiscriminator(final boolean enabled) {
        this.discriminatorEnabled = enabled;
        return this;
    }

    /**
     * Gets a field by its name
     *
     * @param name the name
     * @return the field
     * @throws NoSuchElementException if no value is present
     */
    public FieldModelBuilder<?> fieldModelByFieldName(final String name) throws NoSuchElementException {
        return fieldModels.stream().filter(f -> f.getField().getName().equals(name))
                          .findFirst()
                          .orElseThrow();
    }

    /**
     * @return the fields on this model
     */
    public List<FieldModelBuilder<?>> fieldModels() {
        return fieldModels;
    }

    /**
     * @param type the annotation class
     * @param <A>  the annotation type
     * @return the annotation or null if it doesn't exist
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(final Class<A> type) {
        return (A) annotationsMap.get(type);
    }

    /**
     * The type of this model
     *
     * @return the type
     */
    public Class<T> getType() {
        return type;
    }

    /**
     * @param type the annotation class
     * @return the annotation if it exists
     */
    public boolean hasAnnotation(final Class<? extends Annotation> type) {
        for (Annotation annotation : annotations) {
            if (type.equals(annotation.annotationType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the name of the id field
     */
    public String idFieldName() {
        return idFieldName;
    }

    /**
     * Sets the name of the id field
     *
     * @param name the name
     * @return this
     */
    public EntityModelBuilder<T> idFieldName(final String name) {
        this.idFieldName = name;
        return this;
    }

    /**
     * @return true if the discriminator is enabled
     */
    public boolean isDiscriminatorEnabled() {
        return discriminatorEnabled;
    }

    protected Map<Class<? extends Annotation>, Annotation> annotationsMap() {
        return annotationsMap;
    }

    protected String getCollectionName() {
        Entity entityAn = getAnnotation(Entity.class);
        return entityAn != null && !entityAn.value().equals(Mapper.IGNORED_FIELDNAME)
               ? entityAn.value()
               : datastore.getMapper().getOptions().getCollectionNaming().apply(type.getSimpleName());
    }

    protected Datastore getDatastore() {
        return datastore;
    }

    private void buildHierarchy(final Class<?> type) {
        if (type != null && !type.isEnum() && !type.equals(Object.class)) {
            if (type.isInterface()) {
                interfaces.add(type);
            } else {
                classes.add(type);
            }
            annotations.addAll(Set.of(type.getAnnotations()));
            interfaces.addAll(Set.of(type.getInterfaces()));
            buildHierarchy(type.getSuperclass());

            for (Class<?> anInterface : type.getInterfaces()) {
                buildHierarchy(anInterface);
            }
        }
    }

    private <S> void cachePropertyTypeData(final FieldMetadata<?> metadata,
                                           final Map<String, TypeParameterMap> propertyTypeParameterMap,
                                           final TypeData<S> parentClassTypeData,
                                           final List<String> genericTypeNames,
                                           final Type genericType) {
        TypeParameterMap typeParameterMap = getTypeParameterMap(genericTypeNames, genericType);
        propertyTypeParameterMap.put(metadata.getName(), typeParameterMap);
        metadata.typeParameterInfo(typeParameterMap, parentClassTypeData);
    }

    @SuppressWarnings("ConstantConditions")
    private String getMappedFieldName(final FieldModelBuilder<?> fieldBuilder) {
        MapperOptions options = datastore.getMapper().getOptions();
        if (fieldBuilder.hasAnnotation(Id.class)) {
            return "_id";
        } else if (fieldBuilder.hasAnnotation(Property.class)) {
            final Property mv = fieldBuilder.getAnnotation(Property.class);
            if (!mv.value().equals(Mapper.IGNORED_FIELDNAME)) {
                return mv.value();
            }
        } else if (fieldBuilder.hasAnnotation(Reference.class)) {
            final Reference mr = fieldBuilder.getAnnotation(Reference.class);
            if (!mr.value().equals(Mapper.IGNORED_FIELDNAME)) {
                return mr.value();
            }
        } else if (fieldBuilder.hasAnnotation(Version.class)) {
            final Version me = fieldBuilder.getAnnotation(Version.class);
            if (!me.value().equals(Mapper.IGNORED_FIELDNAME)) {
                return me.value();
            }
        }

        return options.getFieldNaming().apply(fieldBuilder.getName());
    }

    private TypeParameterMap getTypeParameterMap(final List<String> genericTypeNames, final Type propertyType) {
        int classParamIndex = genericTypeNames.indexOf(propertyType.toString());
        TypeParameterMap.Builder builder = TypeParameterMap.builder();
        if (classParamIndex != -1) {
            builder.addIndex(classParamIndex);
        } else {
            if (propertyType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) propertyType;
                for (int i = 0; i < pt.getActualTypeArguments().length; i++) {
                    classParamIndex = genericTypeNames.indexOf(pt.getActualTypeArguments()[i].toString());
                    if (classParamIndex != -1) {
                        builder.addIndex(i, classParamIndex);
                    }
                }
            }
        }
        return builder.build();
    }

    private void processFields(final Class<?> currentClass,
                               final TypeData<?> parentClassTypeData,
                               final List<String> genericTypeNames, final Map<String, TypeParameterMap> propertyTypeParameterMap) {
        for (Field field : currentClass.getDeclaredFields()) {
            final TypeData<?> typeData = TypeData.newInstance(field);

            Type genericType = field.getGenericType();
            TypeParameterMap typeParameterMap = getTypeParameterMap(genericTypeNames, genericType);
            FieldMetadata<?> fieldMetadata = new FieldMetadata<>(field, typeData, typeParameterMap, parentClassTypeData);

            cachePropertyTypeData(fieldMetadata, propertyTypeParameterMap, parentClassTypeData, genericTypeNames, genericType);

            for (Annotation annotation : field.getDeclaredAnnotations()) {
                fieldMetadata.addAnnotation(annotation);
            }
            addModel(createFieldModelBuilder(fieldMetadata));
        }
    }

    private List<String> processTypeNames(final Class<?> currentClass) {
        List<String> genericTypeNames = new ArrayList<>();
        for (TypeVariable<? extends Class<?>> classTypeVariable : currentClass.getTypeParameters()) {
            genericTypeNames.add(classTypeVariable.getName());
        }
        return genericTypeNames;
    }

    /*
        private List<String> processTypeNames(final Class<?> type) {
        List<String> genericTypeNames = new ArrayList<>();
        Type superclass = type.getGenericSuperclass();
        if(superclass != null) {
            genericTypeNames.add()
        }
        for (TypeVariable typeVariable : type.getTypeParameters()) {
            genericTypeNames.add(typeVariable.getName());
        }
        return genericTypeNames;
    }
     */

    <F> FieldModelBuilder<F> createFieldModelBuilder(final FieldMetadata<F> fieldMetadata) {
        FieldModelBuilder<F> fieldModelBuilder = FieldModel.<F>builder()
                                                     .field(fieldMetadata.getField())
                                                     .fieldName(fieldMetadata.getName())
                                                     .typeData(fieldMetadata.getTypeData())
                                                     .annotations(fieldMetadata.getAnnotations());

        fieldModelBuilder.mappedName(getMappedFieldName(fieldModelBuilder));

        if (fieldMetadata.getTypeParameters() != null) {
            fieldModelBuilder.typeData(specializeTypeData(fieldModelBuilder.getTypeData(), fieldMetadata.getTypeParameters(),
                fieldMetadata.getTypeParameterMap()));
        }

        return fieldModelBuilder;
    }
}
