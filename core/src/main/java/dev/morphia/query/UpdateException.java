package dev.morphia.query;

/**
 * Error during update.
 *
 * @author ScottHernandez
 */
public class UpdateException extends RuntimeException {
    /**
     * Creates a UpdateException with a message and a cause
     *
     * @param message the message to record
     */
    public UpdateException(String message) {
        super(message);
    }

    /**
     * Creates a UpdateException with a message and a cause
     *
     * @param message the message to record
     * @param cause   the underlying cause
     */
    public UpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
