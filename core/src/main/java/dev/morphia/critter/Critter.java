package dev.morphia.critter;

import java.lang.reflect.Field;
import java.util.Locale;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Helper methods shared by critter's code generation and the code it generates.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public final class Critter {
    private Critter() {
    }

    /**
     * Returns the package name used for generated Critter classes for the given entity.
     */
    public static String critterPackage(Class<?> entity) {
        // Use the full class name relative to the package (replacing $ with _) so that
        // inner classes with the same simple name in different outer classes don't collide.
        String pkg = entity.getPackageName();
        String relativeName = pkg.isEmpty()
                ? entity.getName()
                : entity.getName().substring(pkg.length() + 1);
        return "%s.__morphia.%s".formatted(pkg, relativeName.replace('$', '_').toLowerCase(Locale.ROOT));
    }

    /**
     * Finds the named field on {@code type} or one of its superclasses and makes it accessible. Generated accessors use
     * this to write {@code final} fields, which {@code putfield} may only do from a constructor.
     *
     * @param type the class to start searching from
     * @param name the field name
     * @return the accessible field
     * @hidden
     * @morphia.internal
     */
    public static Field accessibleField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // keep looking in the superclass
            }
        }
        throw new IllegalArgumentException("No field '%s' found in %s or its superclasses".formatted(name, type.getName()));
    }

    /**
     * Finds the field {@code declaringClass} declares as {@code name} in {@code type}'s hierarchy and makes it accessible.
     * A subclass may declare a field with the same name that isn't mapped (e.g. a transient one), so searching by name
     * alone could find the wrong field. If {@code declaringClass} isn't in the hierarchy (an {@code @ExternalEntity}
     * stand-in describes another class's fields), this falls back to the first field named {@code name}.
     *
     * @param type           the class to start searching from
     * @param declaringClass the binary name of the class that declares the mapped field
     * @param name           the field name
     * @return the accessible field
     * @hidden
     * @morphia.internal
     */
    public static Field accessibleField(Class<?> type, String declaringClass, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (current.getName().equals(declaringClass)) {
                return accessibleField(current, name);
            }
        }
        return accessibleField(type, name);
    }
}
