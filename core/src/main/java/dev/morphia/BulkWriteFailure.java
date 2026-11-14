package dev.morphia;

import com.mongodb.WriteError;
import com.mongodb.lang.Nullable;

/**
 * One entity that a list insert or save did not store.
 *
 * @param entity    the entity, as passed in the list
 * @param index     the entity's position in the list passed to the insert or save
 * @param error     the server's write error (duplicate key, document validation, ...), or null if the failure has none
 * @param exception the failure for entities without a server write error, such as a {@link VersionMismatchException}; null
 *                  when {@code error} is set
 * @see MorphiaBulkWriteException
 * @since 3.0
 */
public record BulkWriteFailure(Object entity, int index, @Nullable WriteError error, @Nullable RuntimeException exception) {
    /**
     * Creates a failure. Exactly one of {@code error} and {@code exception} must be given.
     *
     * @param entity    the entity
     * @param index     the entity's position in the caller's list
     * @param error     the server's write error
     * @param exception the non-server failure
     */
    public BulkWriteFailure {
        if ((error == null) == (exception == null)) {
            throw new IllegalArgumentException("Exactly one of error and exception must be set");
        }
    }
}
