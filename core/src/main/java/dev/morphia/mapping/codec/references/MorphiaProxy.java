package dev.morphia.mapping.codec.references;

import com.mongodb.lang.Nullable;

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
    @Nullable
    <T> T unwrap();
}
