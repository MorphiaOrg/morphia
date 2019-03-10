package dev.morphia.converters;


/**
 * Indicates an error occurred trying to convert a value.
 */
public class ConverterException extends RuntimeException {
    /**
     * Creates the ConverterException.
     *
     * @param msg the exception message
     */
    public ConverterException(final String msg) {
        super(msg);
    }
}
