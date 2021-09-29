package dev.morphia.mapping.codec;

import com.mongodb.lang.Nullable;
import dev.morphia.mapping.codec.pojo.PropertyModel;

/**
 * Marker interface for creators
 *
 * @morphia.internal
 */
public interface MorphiaInstanceCreator {
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
    void set(@Nullable Object value, PropertyModel model);
}
