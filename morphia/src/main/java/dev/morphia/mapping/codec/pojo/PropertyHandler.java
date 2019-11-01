package dev.morphia.mapping.codec.pojo;

/**
 * @morphia.internal
 */
public interface PropertyHandler {
    /**
     * Encodes the value
     *
     * @param value the value
     * @return the encoded value
     */
    Object encode(Object value);
}
