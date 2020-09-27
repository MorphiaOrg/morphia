package dev.morphia.mapping.lazy.proxy;

/**
 * An exception for use with references.
 */
public class ReferenceException extends RuntimeException {
    /**
     * Creates a ReferenceException with the given message
     *
     * @param msg the message to log
     */
    public ReferenceException(String msg) {
        super(msg);
    }
}
