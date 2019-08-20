package dev.morphia.mapping.codec.pojo;

import dev.morphia.mapping.Mapper;
import dev.morphia.sofia.Sofia;
import org.bson.codecs.pojo.ClassModelBuilder;
import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.IdPropertyModelHolder;
import org.bson.codecs.pojo.PropertyModel;
import org.bson.codecs.pojo.PropertyModelBuilder;
import org.bson.codecs.pojo.TypeData;
import org.bson.codecs.pojo.TypeParameterMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.groupingBy;
import static org.bson.codecs.pojo.PojoBuilderHelper.getTypeParameterMap;

public class MorphiaModelBuilder<T> extends ClassModelBuilder<T> {
    private final List<FieldModelBuilder<?>> fieldModelBuilders = new ArrayList<>();
    private final Mapper mapper;

    public MorphiaModelBuilder(final Mapper mapper, final Class<T> type) {
        super(type);
        this.mapper = mapper;
        configureClassModelBuilder();
    }

    protected void configureClassModelBuilder() {
        TypeData<?> parentClassTypeData = null;

        Set<Class<?>> classes = buildHierarchy(getType());

        List<Annotation> annotations = new ArrayList<>();
        for (Class<?> klass : classes) {
            List<String> genericTypeNames = processTypeNames(klass);

            annotations.addAll(asList(klass.getDeclaredAnnotations()));
            processFields(klass,
                parentClassTypeData, genericTypeNames);

            parentClassTypeData = TypeData.newInstance(klass.getGenericSuperclass(), klass);
        }
        annotations(annotations);
    }

    private Set<Class<?>> buildHierarchy(Class<?> clazz) {
        Set<Class<?>> set = new LinkedHashSet<>();
        if (clazz != null && !clazz.isEnum() && !clazz.equals(Object.class)) {
            set.addAll(buildHierarchy(clazz.getSuperclass()));
            for (Class<?> anInterface : clazz.getInterfaces()) {
                set.addAll(buildHierarchy(anInterface));
            }
            set.add(clazz);
        }

        return set;
    }

    private List<String> processTypeNames(Class<?> currentClass) {
        List<String> genericTypeNames = new ArrayList<>();
        for (TypeVariable<? extends Class<?>> classTypeVariable : currentClass.getTypeParameters()) {
            genericTypeNames.add(classTypeVariable.getName());
        }
        return genericTypeNames;
    }

    private void processFields(Class<?> currentClass, TypeData<?> parentClassTypeData, List<String> genericTypeNames) {
        for (Field field : currentClass.getDeclaredFields()) {
            // Note if properties are present and types don't match, the underlying field is treated as an implementation detail.
            final TypeData<?> typeData = TypeData.newInstance(field);

            FieldMetadata<?> fieldMetadata = new FieldMetadata<>(field, typeData);
            TypeParameterMap typeParameterMap = getTypeParameterMap(genericTypeNames, field.getGenericType());
            fieldMetadata.typeParameterInfo(typeParameterMap, parentClassTypeData);
            for (Annotation annotation : field.getDeclaredAnnotations()) {
                fieldMetadata.addAnnotation(annotation);
            }
            fieldModelBuilders.add(createFieldModelBuilder(fieldMetadata));
        }
    }

    public <A extends Annotation> A getAnnotation(Class<A> klass) {
        for (Annotation annotation : getAnnotations()) {
            if (klass.equals(annotation.annotationType())) {
                return klass.cast(annotation);
            }
        }
        return null;
    }

    public boolean hasAnnotation(Class<? extends Annotation> klass) {
        for (Annotation annotation : getAnnotations()) {
            if (klass.equals(annotation.annotationType())) {
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
        PropertyModel<?> idPropertyModel = null;

        for (Convention convention : getConventions()) {
            convention.apply(this);
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

        List<PropertyModel<?>> propertyModels = new ArrayList<>();
        for (final PropertyModelBuilder<?> builder : getPropertyModelBuilders()) {
            PropertyModel<?> propertyModel = builder.build();
            if (propertyModel.getReadName().equals(getIdPropertyName())) {
                idPropertyModel = propertyModel;
            }
            propertyModels.add(propertyModel);
        }

        List<FieldModel<?>> fieldModels = fieldModelBuilders.stream()
                                                            .map(FieldModelBuilder::build)
                                                            .collect(Collectors.toList());
        Map<Class<? extends Annotation>, List<Annotation>> annotations = getAnnotations().stream()
                                 .collect(groupingBy(
                                     annotation -> (Class<? extends Annotation>) annotation.annotationType()));

        return new MorphiaModel<>(mapper, getType(), getPropertyNameToTypeParameterMap(), getInstanceCreatorFactory(), useDiscriminator(),
            getDiscriminatorKey(), getDiscriminator(), IdPropertyModelHolder.create(getType(), idPropertyModel, getIdGenerator()),
            annotations, fieldModels, propertyModels);
    }

    public static <T> FieldModelBuilder<T> createFieldModelBuilder(final FieldMetadata<T> fieldMetadata) {
        FieldModelBuilder<T> fieldModelBuilder = FieldModel.<T>builder()
                                                     .field(fieldMetadata.getField())
                                                     .fieldName(fieldMetadata.getName())
                                                     .readName(fieldMetadata.getName())
                                                     .writeName(fieldMetadata.getName())
                                                     .typeData(fieldMetadata.getTypeData())
                                                     .annotations(fieldMetadata.getAnnotations());

        if (fieldMetadata.getTypeParameters() != null) {
            specializeFieldModelBuilder(fieldModelBuilder, fieldMetadata);
        }

        return fieldModelBuilder;
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
}
