package dev.morphia.mapping.codec.pojo;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
public interface PropertyHandler {
    /**
     * Encodes the value
     *
     * @param value the value
     * @return the encoded value
     */
    Object encode(@Nullable Object value);
}
