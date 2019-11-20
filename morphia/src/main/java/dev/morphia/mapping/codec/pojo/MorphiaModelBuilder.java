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
import org.bson.codecs.pojo.ClassModelBuilder;
import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.PojoBuilderHelper;
import org.bson.codecs.pojo.PropertyMetadata;
import org.bson.codecs.pojo.PropertyModel;
import org.bson.codecs.pojo.PropertyModelBuilder;
import org.bson.codecs.pojo.TypeData;
import org.bson.codecs.pojo.TypeParameterMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static org.bson.codecs.pojo.PojoBuilderHelper.getTypeParameterMap;

/**
 * Builder for MorphiaModels
 * @param <T> the entity type
 */
public class MorphiaModelBuilder<T> extends ClassModelBuilder<T> {
    private final List<FieldModelBuilder<?>> fieldModelBuilders = new ArrayList<>();
    private final Datastore datastore;
    private List<FieldModel<?>> fieldModels;
    private Map<Class<? extends Annotation>, List<Annotation>> annotationsMap;
    private List<PropertyModel<?>> propertyModels;
    private PropertyModel<?> idPropertyModel;

    /**
     * Create a builder
     *
     * @param datastore the datastore to use
     * @param type      the entity type
     */
    public MorphiaModelBuilder(final Datastore datastore, final Class<T> type) {
        super(type);
        this.datastore = datastore;
        configure();
    }

    <T> FieldModelBuilder<T> createFieldModelBuilder(final FieldMetadata<T> fieldMetadata) {
        FieldModelBuilder<T> fieldModelBuilder = FieldModel.<T>builder()
                                                     .field(fieldMetadata.getField())
                                                     .fieldName(fieldMetadata.getName())
                                                     .typeData(fieldMetadata.getTypeData())
                                                     .annotations(fieldMetadata.getAnnotations());

        fieldModelBuilder.mappedName(getMappedFieldName(fieldModelBuilder));

        if (fieldMetadata.getTypeParameters() != null) {
            specializeFieldModelBuilder(fieldModelBuilder, fieldMetadata);
        }

        return fieldModelBuilder;
    }

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

    @SuppressWarnings("unchecked")
    private static <V> void specializeFieldModelBuilder(final FieldModelBuilder<V> fieldModelBuilder, final FieldMetadata<V> metaData) {
        if (metaData.getTypeParameterMap().hasTypeParameters() && !metaData.getTypeParameters().isEmpty()) {
            TypeData<V> specializedType;
            Map<Integer, Integer> fieldToClassParamIndexMap = metaData.getTypeParameterMap().getPropertyToClassParamIndexMap();
            Integer classType = fieldToClassParamIndexMap.get(-1);
            if (classType != null) {
                specializedType = (TypeData<V>) metaData.getTypeParameters().get(classType);
            } else {
                TypeData.Builder<V> builder = TypeData.builder(fieldModelBuilder.getTypeData().getType());
                List<TypeData<?>> parameters = new ArrayList<>(fieldModelBuilder.getTypeData().getTypeParameters());
                for (int i = 0; i < parameters.size(); i++) {
                    for (Map.Entry<Integer, Integer> mapping : fieldToClassParamIndexMap.entrySet()) {
                        if (mapping.getKey().equals(i)) {
                            parameters.set(i, metaData.getTypeParameters().get(mapping.getValue()));
                        }
                    }
                }
                builder.addTypeParameters(parameters);
                specializedType = builder.build();
            }
            fieldModelBuilder.typeData(specializedType);
        }
    }

    /**
     * @param type the annotation class
     * @param <A>  the annotation type
     * @return the annotation or null if it doesn't exist
     */
    public <A extends Annotation> A getAnnotation(final Class<A> type) {
        List<A> annotations = (List<A>) annotationsMap.get(type);
        if (annotations != null && !annotations.isEmpty()) {
            return annotations.get(annotations.size() - 1);
        }
        return null;
    }

    /**
     * @param type the annotation class
     * @return the annotation if it exists
     */
    public boolean hasAnnotation(final Class<? extends Annotation> type) {
        for (Annotation annotation : getAnnotations()) {
            if (type.equals(annotation.annotationType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the fields on the modeled type
     */
    public List<FieldModelBuilder<?>> getFieldModelBuilders() {
        return fieldModelBuilders;
    }

    /**
     * Creates a new ClassModel instance based on the mapping data provided.
     *
     * @return the new instance
     */
    public MorphiaModel<T> build() {
        annotationsMap = getAnnotations().stream()
                                         .collect(groupingBy(a -> (Class<? extends Annotation>) a.annotationType()));

        for (final Convention convention : getConventions()) {
            convention.apply(this);
        }
        idPropertyModel = null;
        idPropertyName(null);
        for (MorphiaConvention convention : datastore.getMapper().getOptions().getConventions()) {
            convention.apply(datastore, this);
        }

        if (useDiscriminator()) {
            Objects.requireNonNull(getDiscriminatorKey(), Sofia.notNull("discriminatorKey"));
            Objects.requireNonNull(getDiscriminator(), Sofia.notNull("discriminator"));
        }

        for (PropertyModelBuilder<?> propertyModelBuilder : getPropertyModelBuilders()) {
            if (propertyModelBuilder.getName().equals(getIdPropertyName())) {
                propertyModelBuilder.readName(ID_PROPERTY_NAME).writeName(ID_PROPERTY_NAME);
            }
        }

        propertyModels = new ArrayList<>();
        for (final PropertyModelBuilder<?> builder : getPropertyModelBuilders()) {
            PropertyModel<?> propertyModel = builder.build();
            if (propertyModel.getReadName().equals(ID_PROPERTY_NAME)) {
                idPropertyModel = propertyModel;
            }
            propertyModels.add(propertyModel);
        }

        fieldModels = fieldModelBuilders.stream()
                                        .map(FieldModelBuilder::build)
                                        .collect(Collectors.toList());

        if (getIdPropertyName() == null) {
            idGenerator(null);
        }

        return new MorphiaModel<>(this);
    }

    protected String getCollectionName() {
        Entity entityAn = getAnnotation(Entity.class);
        return entityAn != null && !entityAn.value().equals(Mapper.IGNORED_FIELDNAME)
               ? entityAn.value()
               : datastore.getMapper().getOptions().getCollectionNaming().apply(getType().getSimpleName());
    }

    protected Datastore getDatastore() {
        return datastore;
    }

    protected List<FieldModel<?>> getFieldModels() {
        return fieldModels;
    }

    protected Map<Class<? extends Annotation>, List<Annotation>> getAnnotationsMap() {
        return annotationsMap;
    }

    protected List<PropertyModel<?>> getPropertyModels() {
        return propertyModels;
    }

    protected PropertyModel<?> getIdPropertyModel() {
        return idPropertyModel;
    }

    protected void configure() {
        TypeData<?> parentClassTypeData = null;
        Set<Class<?>> classes = buildHierarchy(getType());

        List<Annotation> annotations = new ArrayList<>();
        for (Class<?> klass : classes) {
            List<String> genericTypeNames = processTypeNames(klass);

            annotations.addAll(List.of(klass.getDeclaredAnnotations()));
            processFields(klass, parentClassTypeData, genericTypeNames);

            parentClassTypeData = TypeData.newInstance(klass.getGenericSuperclass(), klass);
        }
        annotations(annotations);
    }

    private Set<Class<?>> buildHierarchy(final Class<?> type) {
        Set<Class<?>> set = new LinkedHashSet<>();
        if (type != null && !type.isEnum() && !type.equals(Object.class)) {
            set.addAll(buildHierarchy(type.getSuperclass()));
            for (Class<?> anInterface : type.getInterfaces()) {
                set.addAll(buildHierarchy(anInterface));
            }
            set.add(type);
        }

        return set;
    }

    private List<String> processTypeNames(final Class<?> currentClass) {
        List<String> genericTypeNames = new ArrayList<>();
        for (TypeVariable<? extends Class<?>> classTypeVariable : currentClass.getTypeParameters()) {
            genericTypeNames.add(classTypeVariable.getName());
        }
        return genericTypeNames;
    }

    private void processFields(final Class<?> currentClass, final TypeData<?> parentClassTypeData, final List<String> genericTypeNames) {
        Map<String, PropertyMetadata<?>> propertyNameMap = new HashMap<>();
        Map<String, TypeParameterMap> propertyTypeParameterMap = new HashMap<>();

        for (Field field : currentClass.getDeclaredFields()) {
            PropertyMetadata<?> propertyMetadata = PojoBuilderHelper.getOrCreateFieldPropertyMetadata(field.getName(),
                getType().getSimpleName(), propertyNameMap, TypeData.newInstance(field), propertyTypeParameterMap, parentClassTypeData,
                genericTypeNames, field.getGenericType());
            if (propertyMetadata != null && propertyMetadata.getField() == null) {
                propertyMetadata.field(field);
                for (Annotation annotation : field.getDeclaredAnnotations()) {
                    propertyMetadata.addReadAnnotation(annotation);
                    propertyMetadata.addWriteAnnotation(annotation);
                }
            }

            final TypeData<?> typeData = TypeData.newInstance(field);

            FieldMetadata<?> fieldMetadata = new FieldMetadata<>(field, typeData,
                getTypeParameterMap(genericTypeNames, field.getGenericType()), parentClassTypeData);
            for (Annotation annotation : field.getDeclaredAnnotations()) {
                fieldMetadata.addAnnotation(annotation);
            }
            fieldModelBuilders.add(createFieldModelBuilder(fieldMetadata));
        }
    }
}
