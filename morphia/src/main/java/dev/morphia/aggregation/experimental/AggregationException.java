package dev.morphia.aggregation.experimental;

public class AggregationException extends RuntimeException {
    public AggregationException(final String message, final Exception cause) {
        super(message, cause);
    }
}
