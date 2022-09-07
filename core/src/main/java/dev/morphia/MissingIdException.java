package dev.morphia;

import dev.morphia.sofia.Sofia;

public class MissingIdException extends RuntimeException {
    MissingIdException() {
        super(Sofia.missingIdOnReplace());
    }
}
