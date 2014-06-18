package org.mongodb.morphia.query.validation;

public class ValidationFailure {
    private final String message;

    public ValidationFailure(final String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Validation failed: '" + message + "'";
    }
}
