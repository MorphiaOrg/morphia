package dev.morphia.critter.parser.java;

import java.util.List;

import dev.morphia.critter.Critter;

/**
 * Singleton parser providing annotation descriptor resolution utilities.
 */
public class CritterParser {
    /** The singleton instance of this parser. */
    public static final CritterParser INSTANCE = new CritterParser();

    private CritterParser() {
    }

    /**
     * Returns the descriptors of all annotation types that mark a field or method as a mapped property.
     */
    public List<String> propertyAnnotations() {
        return Critter.propertyAnnotations;
    }

    /**
     * Returns the descriptors of all annotation types that mark a field or method as transient (not persisted).
     */
    public List<String> transientAnnotations() {
        return Critter.transientAnnotations;
    }
}
