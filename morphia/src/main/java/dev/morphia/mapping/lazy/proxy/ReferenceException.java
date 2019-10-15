package dev.morphia.mapping.lazy.proxy;

import java.util.ConcurrentModificationException;

public class ReferenceException extends RuntimeException {
    /**
     * Creates a ReferenceException with the given message
     *
     * @param msg the message to log
     */
    public ReferenceException(final String msg) {
        super(msg);
    }
}
