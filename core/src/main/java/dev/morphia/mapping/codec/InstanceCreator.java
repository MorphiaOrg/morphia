package dev.morphia.mapping.codec;

import dev.morphia.mapping.codec.pojo.FieldModel;

/**
 * Creates instances of types.
 */
public interface InstanceCreator {

    /**
     * @return the new class instance.
     */
    Object getInstance();

    /**
     * Sets a value for the given FieldModel
     *
     * @param value the value
     * @param model the model
     */
    void set(Object value, FieldModel model);

}
