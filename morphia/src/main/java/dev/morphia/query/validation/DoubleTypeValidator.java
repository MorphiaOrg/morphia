package dev.morphia.query.validation;

import java.util.List;

import static java.lang.String.format;

/**
 * Validation for fields of Double/double type.  Allows numbers that could be compared against double as the values.
 */
public final class DoubleTypeValidator extends TypeValidator {
    private static final DoubleTypeValidator INSTANCE = new DoubleTypeValidator();

    private DoubleTypeValidator() {
    }

    /**
     * Get the instance.
     *
     * @return the Singleton instance of this validator
     */
    public static DoubleTypeValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean appliesTo(final Class<?> type) {
        return type == double.class || type == Double.class;
    }

    @Override
    protected void validate(final Class<?> type, final Object value, final List<ValidationFailure> validationFailures) {
        if (!(value instanceof Integer || value instanceof Long || value instanceof Double)) {
            validationFailures.add(new ValidationFailure(format("When type is a double the value should be compatible with double.  "
                                                                + "Type was %s and value '%s' was a %s", type.getCanonicalName(), value,
                                                                value.getClass().getCanonicalName()
                                                               )));
        }
    }
}
