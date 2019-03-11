package dev.morphia.query.validation;

import java.util.List;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Validates query values that are Pattern to check the field type is a String.
 */
public final class PatternValueValidator extends ValueValidator {
    private static final PatternValueValidator INSTANCE = new PatternValueValidator();

    private PatternValueValidator() {
    }

    /**
     * Get the instance.
     *
     * @return the Singleton instance of this validator
     */
    public static PatternValueValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected Class getRequiredValueType() {
        return Pattern.class;
    }

    @Override
    protected void validate(final Class<?> type, final Object value, final List<ValidationFailure> validationFailures) {
        if (!String.class.equals(type)) {
            validationFailures.add(new ValidationFailure(format("Patterns can only be used as query values for Strings")));
        }
    }
}
