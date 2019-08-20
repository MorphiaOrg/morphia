package dev.morphia.mapping.codec.pojo;

import dev.morphia.annotations.Entity;
import dev.morphia.mapping.Mapper;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.IdPropertyModelHolder;
import org.bson.codecs.pojo.InstanceCreatorFactory;
import org.bson.codecs.pojo.PropertyModel;
import org.bson.codecs.pojo.TypeParameterMap;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.stream.Collectors.groupingBy;

public class MorphiaModel<T> extends ClassModel<T> {
    private final Mapper mapper;
    private final Map<Class<? extends Annotation>, List<Annotation>> annotations;
    private final List<FieldModel<?>> fieldModels;
    private String collectionName;

    public MorphiaModel(final Mapper mapper,
                        final Class<T> clazz,
                        final Map<String, TypeParameterMap> propertyNameToTypeParameterMap,
                        final InstanceCreatorFactory<T> instanceCreatorFactory,
                        final Boolean discriminatorEnabled,
                        final String discriminatorKey,
                        final String discriminator,
                        final IdPropertyModelHolder<?> idPropertyModelHolder,
                        final List<Annotation> annotations,
                        final List<FieldModel<?>> fieldModels,
                        final List<PropertyModel<?>> propertyModels) {
        super(clazz, propertyNameToTypeParameterMap, instanceCreatorFactory, discriminatorEnabled, discriminatorKey, discriminator,
            idPropertyModelHolder, propertyModels);
        this.mapper = mapper;
        this.annotations = annotations.stream()
                                      .collect(groupingBy(
                                          annotation -> (Class<? extends Annotation>) annotation.annotationType()));
        this.fieldModels = fieldModels;
    }

    public MorphiaModel(final Mapper mapper,
                        final Class<T> clazz,
                        final Map<String, TypeParameterMap> propertyNameToTypeParameterMap,
                        final InstanceCreatorFactory<T> instanceCreatorFactory,
                        final Boolean discriminatorEnabled,
                        final String discriminatorKey,
                        final String discriminator,
                        final IdPropertyModelHolder<?> idPropertyModelHolder,
                        final Map<Class<? extends Annotation>, List<Annotation>> annotations,
                        final List<FieldModel<?>> fieldModels,
                        final List<PropertyModel<?>> propertyModels) {
        super(clazz, propertyNameToTypeParameterMap, instanceCreatorFactory, discriminatorEnabled, discriminatorKey, discriminator,
            idPropertyModelHolder, propertyModels);
        this.mapper = mapper;
        this.annotations = annotations;
        this.fieldModels = fieldModels;
    }

    /**
     * Returns all the annotations on this model
     *
     * @return the list of annotations
     */
    public Map<Class<? extends Annotation>, List<Annotation>> getAnnotations() {
        return annotations;
    }

    /**
     * Returns all the fields on this model
     *
     * @return the list of fields
     */
    public List<FieldModel<?>> getFieldModels() {
        return fieldModels;
    }

    public <A> A getAnnotation(final Class<? extends Annotation> clazz) {
        final List<Annotation> found = annotations.get(clazz);
        return found == null || found.isEmpty() ? null : (A) found.get(found.size() - 1);
    }

    public <A> List<A> getAnnotations(final Class<? extends Annotation> clazz) {
        return (List<A>) annotations.get(clazz);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MorphiaModel.class.getSimpleName() + "[", "]")
                   .add("name='" + getName() + "'")
                   .add("type=" + getType())
                   .add("annotations=" + annotations)
                   .add("hasTypeParameters=" + hasTypeParameters())
                   .add("useDiscriminator=" + useDiscriminator())
                   .add("discriminatorKey='" + getDiscriminatorKey() + "'")
                   .add("discriminator='" + getDiscriminator() + "'")
                   .add("fieldModels=" + fieldModels)
                   .toString();
    }

    public String getCollectionName() {
        if(collectionName == null) {
            Entity entityAn = getAnnotation(Entity.class);
            if(entityAn != null) {
                collectionName = null;
                if (entityAn.value().equals(Mapper.IGNORED_FIELDNAME)) {
                    return mapper.getOptions().isUseLowerCaseCollectionNames() ? getType().getSimpleName().toLowerCase() :
                           getType().getSimpleName();
                } else {
                    return entityAn.value();
                }
            }
        }
        return collectionName;
    }
}
