package dev.morphia.query.validation;

/**
 * Represents a validation failure.
 */
public class ValidationFailure {
    private final String message;

    /**
     * Creates a ValidationFailure with a message
     *
     * @param message the reason for the failure
     */
    public ValidationFailure(final String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Validation failed: '" + message + "'";
    }
}
