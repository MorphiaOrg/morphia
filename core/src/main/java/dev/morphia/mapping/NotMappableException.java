package dev.morphia.mapping;

import dev.morphia.sofia.Sofia;

/**
 * Indicates a type is not mappable by Morphia
 *
 * @since 2.2
 */
public final class NotMappableException extends RuntimeException {
    public NotMappableException(Class type) {
        super(Sofia.notMappable(type.getName()));
    }
}
