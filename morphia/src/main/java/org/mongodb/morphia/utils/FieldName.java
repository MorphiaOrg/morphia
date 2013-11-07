package org.mongodb.morphia.utils;


import java.lang.reflect.Field;


/**
 * Handy class to test if a certain FieldName is available in a class. Usage: If you add {@code public static final String _foo =
 * FieldName.of("foo"); }
 * <p/>
 * you´ll see an Exception on loading the class. A nice side-effect: if you use this in Queries like {@code
 * q.field(MyEntity._foo).equal("bar") }
 * <p/>
 * your IDE is able to track this usage. Using FieldName does not at all replace query validation.
 *
 * @author us@thomas-daily.de
 */
public final class FieldName {
    private FieldName() {
    }

    public static String of(final String name) {
        return of(callingClass(), name);
    }

    public static String of(final Class<?> clazz, final String name) {
        Assert.parameterNotNull(clazz, "clazz");
        Assert.parameterNotNull(name, "name");
        if (hasField(clazz, name)) {
            return name;
        }
        throw new FieldNameNotFoundException("Field called '" + name + "' on class '" + clazz + "' was not found.");
    }

    private static boolean hasField(final Class<?> clazz, final String name) {
        final Field[] fa = ReflectionUtils.getDeclaredAndInheritedFields(clazz, true);
        for (final Field field : fa) {
            if (name.equals(field.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @throws IllegalStateException
     */
    private static Class<?> callingClass() {
        final StackTraceElement[] stackTrace = new Exception().getStackTrace();
        for (final StackTraceElement e : stackTrace) {
            final String c = e.getClassName();

            if (!c.equals(FieldName.class.getName())) {
                return forName(c);
            }
        }
        throw new IllegalStateException();

    }

    private static Class<?> forName(final String c) {
        try {
            return Class.forName(c);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error when getting class for name '" + c + "'");
        }
    }

    public static class FieldNameNotFoundException extends RuntimeException {
        public FieldNameNotFoundException(final String msg) {
            super(msg);
        }
    }

}
