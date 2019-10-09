package dev.morphia.mapping.codec.pojo;

/**
 * @morphia.internal
 */
public interface PropertyHandler {
    Object prepare(Object value);
}
