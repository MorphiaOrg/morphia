package dev.morphia.query.validation;

import java.util.List;

import static java.lang.String.format;

/**
 * If the Type is some sort of integer-compatible field (see {@code getTypeClasses}) then this validator will check if the value is of the
 * correct type for this field.
 */
public final class IntegerTypeValidator extends TypeValidator {
    private static final IntegerTypeValidator INSTANCE = new IntegerTypeValidator();

    private IntegerTypeValidator() {
    }

    /**
     * Get the instance.
     *
     * @return the Singleton instance of this validator
     */
    public static IntegerTypeValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean appliesTo(final Class<?> type) {
        return type == int.class || type == Integer.class;
    }

    @Override
    protected void validate(final Class<?> type, final Object value, final List<ValidationFailure> validationFailures) {
        if (Integer.class != value.getClass()) {
            validationFailures.add(new ValidationFailure(format("When type is one of the integer types the value should be an Integer.  "
                                                                + "Type was %s and value '%s' was a %s", type, value, value.getClass())));
        }
    }
}
