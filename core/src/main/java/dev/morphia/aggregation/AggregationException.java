package dev.morphia.aggregation;

/**
 * Indicates a failure in a pipeline execution
 */
public class AggregationException extends RuntimeException {
    /**
     * Creates an exception with a message
     *
     * @param message the message
     */
    public AggregationException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and a cause
     *
     * @param message the message
     * @param cause   the cause
     */
    public AggregationException(String message, Exception cause) {
        super(message, cause);
    }
}
