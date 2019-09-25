package dev.morphia.mapping.codec.references;

/**
 * @morphia.internal
 */
public interface MorphiaProxy {
    boolean isFetched();

    <T> T unwrap();
}
