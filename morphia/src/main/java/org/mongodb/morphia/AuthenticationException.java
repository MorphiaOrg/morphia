package org.mongodb.morphia;


/**
 * Signifies an error trying to authenticate against the database.
 */
public class AuthenticationException extends RuntimeException {

    /**
     * Creates an instance with the given message
     *
     * @param message the message
     */
    public AuthenticationException(final String message) {
        super(message);
    }
}
