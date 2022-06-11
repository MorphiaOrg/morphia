package dev.morphia.internal;

import com.mongodb.client.ClientSession;
import com.mongodb.lang.Nullable;

/**
 * Marks an options class as having a configurable ClientSession
 *
 * @param <T> the type of the SessionConfigurable
 * @morphia.internal
 * @since 2.0
 */
public interface SessionConfigurable<T extends SessionConfigurable<T>> {
    /**
     * Set the client session to use for the insert.
     *
     * @param clientSession the client session
     * @return this
     * @since 2.0
     */
    T clientSession(ClientSession clientSession);

    /**
     * The client session to use for the insertion.
     *
     * @return the client session
     * @since 2.0
     */
    @Nullable
    ClientSession clientSession();
}
