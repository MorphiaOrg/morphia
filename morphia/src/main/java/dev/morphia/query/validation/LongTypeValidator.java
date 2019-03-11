package dev.morphia.query.validation;

import java.util.List;

import static java.lang.String.format;

/**
 * Checks the type of the value if the type of the field is a Long or long.
 */
public class LongTypeValidator extends TypeValidator {
    private static final LongTypeValidator INSTANCE = new LongTypeValidator();

    /**
     * Get the instance.
     *
     * @return the Singleton instance of this validator
     */
    public static LongTypeValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean appliesTo(final Class<?> type) {
        return type == long.class || type == Long.class;
    }

    @Override
    protected void validate(final Class<?> type, final Object value, final List<ValidationFailure> validationFailures) {
        if (value.getClass() != long.class
            && value.getClass() != Long.class
            && value.getClass() != int.class
            && value.getClass() != Integer.class) {
            validationFailures.add(new ValidationFailure(format("When type is a long the value should be a long or integer.  "
                                                                + "Type was %s and value '%s' was a %s", type, value, value.getClass())));
        }

    }
}
