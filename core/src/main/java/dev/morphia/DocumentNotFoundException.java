package dev.morphia;

import dev.morphia.mapping.MappingException;

/**
 * This exception is thrown when a replace finds no document to replace, either because no document has the entity's ID or because the
 * entity's shard key values do not match the stored document.
 *
 * @since 3.0
 */
public class DocumentNotFoundException extends MappingException {

    /**
     * Creates a new exception with a message.
     *
     * @param message the message to record
     */
    public DocumentNotFoundException(String message) {
        super(message);
    }
}
