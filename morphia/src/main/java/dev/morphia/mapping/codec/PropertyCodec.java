package dev.morphia.mapping.codec;

import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.codec.pojo.FieldModel;
import org.bson.codecs.Codec;
import org.bson.codecs.pojo.TypeData;

import java.lang.reflect.Field;

/**
 * Defines codecs for properties
 * @param <T> the property type
 * @morphia.internal
 */
public abstract class PropertyCodec<T> implements Codec<T> {
    private final Field field;
    private final TypeData typeData;
    private MappedClass mappedClass;
    private Datastore datastore;

    /**
     * Creates a codec
     *
     * @param datastore    the datastore
     * @param field        the reference field
     * @param typeData     the field type data
     */
    public PropertyCodec(final Datastore datastore, final Field field, final TypeData typeData) {
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

    protected MappedClass getFieldMappedClass() {
        if (mappedClass == null) {
            mappedClass = datastore.getMapper().getMappedClass(FieldModel.normalize(typeData));
        }
        return mappedClass;
    }

}
