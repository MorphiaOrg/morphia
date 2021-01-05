package dev.morphia.mapping.codec;

import dev.morphia.Datastore;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import org.bson.codecs.Codec;

import java.lang.reflect.Field;

/**
 * Defines codecs for properties
 * @param <T> the property type
 * @morphia.internal
 */
public abstract class PropertyCodec<T> implements Codec<T> {
    private final Field field;
    private final TypeData typeData;
    private EntityModel entityModel;
    private FieldModel fieldModel;
    private final Datastore datastore;

    /**
     * Creates a codec
     *
     * @param datastore    the datastore
     * @param field        the reference field
     * @param typeData     the field type data
     */
    public PropertyCodec(Datastore datastore, Field field, TypeData typeData) {
        this.datastore = datastore;
        this.field = field;
        this.typeData = typeData;
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
    public Field getField() {
        return field;
    }

    /**
     * @return the type data
     */
    public TypeData getTypeData() {
        return typeData;
    }

    protected EntityModel getEntityModelForField() {
        if (entityModel == null) {
            entityModel = datastore.getMapper().getEntityModel(FieldModel.normalize(typeData));
        }
        return entityModel;
    }

    protected FieldModel getFieldModel() {
        if (fieldModel == null) {
            fieldModel = datastore.getMapper().getEntityModel(field.getDeclaringClass()).getField(field.getName());
        }
        return fieldModel;
    }

}
