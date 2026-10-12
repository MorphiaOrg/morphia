package dev.morphia.mapping.codec;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.pojo.PropertyModel;

/**
 * Marker interface for creators
 *
 * @morphia.internal
 */
@MorphiaInternal
public interface MorphiaInstanceCreator {
    /**
     * @return the new class instance.
     */
    Object getInstance();

    /**
     * Reports whether {@link #getInstance()} may be called before any property has been decoded. That is
     * only true for creators that do not need the decoded values to build the instance -- a creator backed
     * by a no-arg constructor, for example. A creator that feeds decoded values to a constructor must not
     * be asked for an instance early, since the constructor would then run against placeholder values.
     *
     * <p>
     * Decoding uses this to decide whether an entity can be published to a
     * {@link dev.morphia.mapping.codec.DecodeSession} before its properties are read, which is what makes
     * reference cycles terminate. Creators that return {@code false} are registered after decoding instead,
     * so they still deduplicate but cannot break a cycle.
     *
     * @return true if an instance can be created before properties are decoded
     * @since 3.0
     */
    default boolean isEagerInstanceSafe() {
        return false;
    }

    /**
     * Sets a value for the given FieldModel
     *
     * @param value the value
     * @param model the model
     */
    void set(@Nullable Object value, PropertyModel model);
}
