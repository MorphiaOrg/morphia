package dev.morphia.critter.parser.generator;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Thrown when runtime generation can't define a hidden nestmate accessor for an entity because Morphia doesn't have full
 * privilege access to the entity's class. That happens when the entity isn't in Morphia's module: on the classpath, when
 * it's loaded by a different class loader than Morphia (e.g. Morphia in an app server's shared lib and the entities in a
 * webapp), or when it's in a separate JPMS module. Pre-generated models from critter-maven aren't affected.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class NestmateAccessException extends RuntimeException {
    private final transient Class<?> type;

    /**
     * @param type  the class the nestmate accessor was to be defined in
     * @param cause the access failure
     */
    public NestmateAccessException(Class<?> type, IllegalAccessException cause) {
        super("Morphia can't define a nestmate accessor in %s: %s".formatted(type.getName(), cause.getMessage()), cause);
        this.type = type;
    }

    /**
     * @return the class the nestmate accessor was to be defined in
     */
    public Class<?> getType() {
        return type;
    }
}
