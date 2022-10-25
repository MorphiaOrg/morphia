package dev.morphia.mapping.codec;

import java.util.Objects;

import dev.morphia.Datastore;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.bson.codecs.Codec;

/**
 * Defines codecs for properties
 *
 * @param <T> the property type
 * @morphia.internal
 */
@MorphiaInternal
public abstract class BaseReferenceCodec<T> implements Codec<T> {
    private final PropertyModel property;
    private final Mapper mapper;
    private EntityModel entityModel;
    private final Datastore datastore;

    /**
     * Creates a codec
     *
     * @param datastore the datastore
     * @param property  the property
     */
    public BaseReferenceCodec(Datastore datastore, PropertyModel property) {
        this.datastore = datastore;
        this.property = property;
        mapper = datastore.getMapper();
    }

    /**
     * @return the datastore
     */
    public Datastore getDatastore() {
        return datastore;
    }

    /**
     * @return the field
     */
    public PropertyModel getPropertyModel() {
        return property;
    }

    /**
     * @return the type data
     */
    public TypeData<?> getTypeData() {
        return property.getTypeData();
    }

    protected EntityModel getEntityModelForField() {
        if (entityModel == null) {
            entityModel = mapper.getEntityModel(PropertyModel.normalize(getTypeData()));
        }
        return entityModel;
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    protected static Reference getReferenceAnnotation(PropertyModel model) {
        return Objects.requireNonNull(model.getAnnotation(Reference.class));
    }
}
