package dev.morphia;

import dev.morphia.sofia.Sofia;

/**
 * Thrown when an ID value is expected but not found.
 */
public class MissingIdException extends RuntimeException {
    MissingIdException() {
        super(Sofia.missingIdOnReplace());
    }
}
