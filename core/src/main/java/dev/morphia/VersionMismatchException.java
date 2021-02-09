package dev.morphia;

import com.mongodb.lang.Nullable;
import dev.morphia.sofia.Sofia;

import java.util.ConcurrentModificationException;

/**
 * This exception is thrown when a version field does not match the expected state in the database.  It's thrown when a versioned entity
 * is changed by another process after it was loaded but before any changes were written back to the database.
 *
 * @since 2.2
 */
public class VersionMismatchException extends ConcurrentModificationException {

    /**
     * Creates a new exception with a message.
     *
     * @param type    the versioned type
     * @param idValue the ID value
     */
    public VersionMismatchException(Class<?> type, @Nullable Object idValue) {
        super(Sofia.concurrentModification(type.getName(), idValue));
    }
}
