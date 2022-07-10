package dev.morphia;

import dev.morphia.sofia.Sofia;

public class MissingIdException extends RuntimeException {
    public MissingIdException() {
        super(Sofia.missingIdOnReplace());
    }
}
