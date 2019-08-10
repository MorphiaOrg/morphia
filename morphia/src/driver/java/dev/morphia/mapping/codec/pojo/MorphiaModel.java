package dev.morphia.mapping.codec.pojo;

import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.IdPropertyModelHolder;
import org.bson.codecs.pojo.InstanceCreatorFactory;
import org.bson.codecs.pojo.PropertyModel;
import org.bson.codecs.pojo.TypeParameterMap;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class MorphiaModel<T> extends ClassModel<T> {
    private final List<Annotation> annotations;
    private final List<FieldModel<?>> fieldModels;

    public MorphiaModel(final Class<T> clazz,
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

        this.annotations = annotations;
        this.fieldModels = fieldModels;
    }

    /**
     * Gets a {@link FieldModel} by the field name.
     *
     * @param fieldName the FieldModel's field name
     * @return the FieldModel or null if the field is not found
     */
    public FieldModel<?> getFieldModel(final String fieldName) {
        for (FieldModel<?> fieldModel : fieldModels) {
            if (fieldModel.getName().equals(fieldName)) {
                return fieldModel;
            }
        }
        return null;
    }

    /**
     * Returns all the annotations on this model
     *
     * @return the list of annotations
     */
    public List<Annotation> getAnnotations() {
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
}
