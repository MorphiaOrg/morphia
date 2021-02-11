package dev.morphia.mapping.codec;

import dev.morphia.Datastore;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import org.bson.codecs.Codec;

/**
 * Defines codecs for properties
 *
 * @param <T> the property type
 * @morphia.internal
 */
public abstract class BaseReferenceCodec<T> implements Codec<T> {
    private final PropertyModel property;
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
            entityModel = datastore.getMapper().getEntityModel(PropertyModel.normalize(getTypeData()));
        }
        return entityModel;
    }
}
