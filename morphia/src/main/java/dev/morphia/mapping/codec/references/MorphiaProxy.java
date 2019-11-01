package dev.morphia.mapping.codec.references;

/**
 * @morphia.internal
 */
public interface MorphiaProxy {
    /**
     * @return true if the reference has been fetched
     */
    boolean isFetched();

    /**
     * @param <T> the reference type
     * @return the bare reference
     */
    <T> T unwrap();
}
