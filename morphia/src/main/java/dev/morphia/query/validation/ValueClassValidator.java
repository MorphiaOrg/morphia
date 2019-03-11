package dev.morphia.query.validation;

/**
 * Checks that the given value is of the required type.
 */
final class ValueClassValidator implements Validator {
    private ValueClassValidator() {
    }

    /**
     * @param value         a non-null value
     * @param requiredClass a non-null type to validate against
     */
    static boolean valueIsClassOrSubclassOf(final Object value,
                                            final Class<?> requiredClass) {
        return (requiredClass.isAssignableFrom(value.getClass()));
    }

}
