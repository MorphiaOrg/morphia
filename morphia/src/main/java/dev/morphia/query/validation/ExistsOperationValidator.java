package dev.morphia.query.validation;

import dev.morphia.mapping.MappedField;

import java.util.List;

import static java.lang.String.format;

/**
 * Checks if the value can have the {@code FilterOperator.EXISTS} operator applied to it.  Since this class does not need state, and the
 * methods can't be static because it implements an interface, it seems to be one of the few places where the Singleton pattern seems
 * appropriate.
 */
@SuppressWarnings("removal")
public final class ExistsOperationValidator extends OperationValidator {
    private static final ExistsOperationValidator INSTANCE = new ExistsOperationValidator();

    private ExistsOperationValidator() {
    }

    /**
     * Get the instance.
     *
     * @return the Singleton instance of this validator
     */
    public static ExistsOperationValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected dev.morphia.query.FilterOperator getOperator() {
        return dev.morphia.query.FilterOperator.EXISTS;
    }

    @Override
    protected void validate(final MappedField mappedField, final Object value, final List<ValidationFailure> validationFailures) {
        if (value.getClass() != Boolean.class) {
            validationFailures.add(new ValidationFailure(format("For an $exists operation, value '%s' should be a boolean type.  "
                                                                + "Instead it was a: %s", value, value.getClass())));
        }
    }
}
