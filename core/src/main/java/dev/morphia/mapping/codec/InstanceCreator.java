package dev.morphia.mapping.codec;

import dev.morphia.mapping.codec.pojo.FieldModel;

/**
 * Creates instances of types.
 *
 * @param <T> the type of the class
 */
public interface InstanceCreator<T> {

    /**
     * @return the new class instance.
     */
    T getInstance();

    /**
     * Sets a value for the given FieldModel
     *
     * @param value the value
     * @param model the model
     * @param <F>   the model's type
     */
    <F> void set(F value, FieldModel<F> model);

}
