package dev.morphia.mapping.lazy.proxy;


import java.util.ConcurrentModificationException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class LazyReferenceFetchingException extends ConcurrentModificationException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a LazyReferenceFetchingException with the given message
     *
     * @param msg the message to log
     */
    public LazyReferenceFetchingException(final String msg) {
        super(msg);
    }
}
