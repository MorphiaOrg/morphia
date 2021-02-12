package dev.morphia.mapping.codec.pojo;

import com.mongodb.lang.Nullable;

/**
 * @morphia.internal
 * @since 2.0
 */
public interface PropertyHandler {
    /**
     * Encodes the value
     *
     * @param value the value
     * @return the encoded value
     */
    Object encode(@Nullable Object value);
}
