package dev.morphia.mapping;

/**
 * Selects the mapper implementation Morphia uses to map entity classes.
 *
 * @since 3.0
 */
public enum MapperType {
    /**
     * The default reflection-based mapper.
     */
    LEGACY,

    /**
     * The bytecode-generated mapper using critter. Requires critter dependencies on the classpath.
     */
    CRITTER
}
