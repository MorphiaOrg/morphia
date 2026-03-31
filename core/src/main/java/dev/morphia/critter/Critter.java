package dev.morphia.critter;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Transient;

import org.objectweb.asm.Type;

/**
 * Core utility class for Critter code generation, providing shared constants and helper methods.
 */
public class Critter {
    /** Annotation types that mark a field or method as a mapped property. */
    public static final List<Type> propertyAnnotations = new ArrayList<>(List.of(Type.getType(Property.class)));
    /** Annotation types that mark a field or method as transient (not persisted). */
    public static final List<Type> transientAnnotations = new ArrayList<>(List.of(Type.getType(Transient.class)));

    /**
     * Returns the package name used for generated Critter classes for the given entity.
     *
     * @param entity the entity class
     * @return the generated package name
     */
    public static String critterPackage(Class<?> entity) {
        return "%s.__morphia.%s".formatted(entity.getPackageName(), entity.getSimpleName().toLowerCase());
    }

    /**
     * Converts a string to title case by capitalizing the first character.
     *
     * @param s the input string
     * @return the string with the first character upper-cased, or the original string if null or empty
     */
    public static String titleCase(String s) {
        if (s == null || s.isEmpty())
            return s;
        return "%c%s".formatted(Character.toUpperCase(s.charAt(0)), s.substring(1));
    }

    /**
     * Converts a string to identifier (camel) case by lower-casing the first character.
     *
     * @param s the input string
     * @return the string with the first character lower-cased, or the original string if null or empty
     */
    public static String identifierCase(String s) {
        if (s == null || s.isEmpty())
            return s;
        return "%c%s".formatted(Character.toLowerCase(s.charAt(0)), s.substring(1));
    }

    private final File root;
    private final File outputDir;
    private final File ksp;

    /**
     * Creates a new Critter instance rooted at the given directory.
     *
     * @param root the root directory for source files
     */
    public Critter(File root) {
        this.root = root;
        this.outputDir = new File(root, "target");
        this.ksp = new File(outputDir, "ksp");
    }

    private URI loadPath() throws Exception {
        return Entity.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    }
}
