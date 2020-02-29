package dev.morphia.query.validation;

import dev.morphia.mapping.MappedField;

import java.util.List;

import static dev.morphia.query.validation.CollectionTypeValidator.typeIsIterableOrArrayOrMap;
import static java.lang.String.format;

/**
 * Validates a query that uses the FilterOperator.ALL operator.
 */
@SuppressWarnings("removal")
public final class AllOperationValidator extends OperationValidator {
    private static final AllOperationValidator INSTANCE = new AllOperationValidator();

    private AllOperationValidator() {
    }

    /**
     * Get the instance.
     *
     * @return the Singleton instance of this validator
     */
    public static AllOperationValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected dev.morphia.query.FilterOperator getOperator() {
        return dev.morphia.query.FilterOperator.ALL;
    }

    @Override
    protected void validate(final MappedField mappedField, final Object value, final List<ValidationFailure> validationFailures) {
        if (value == null) {
            validationFailures.add(new ValidationFailure(format("For an $all operation, value cannot be null.")));
        } else if (!typeIsIterableOrArrayOrMap(value.getClass())) {
            validationFailures.add(new ValidationFailure(format("For an $all operation, value '%s' should be an array, "
                                                                + "an Iterable, or a Map.  Instead it was a: %s",
                                                                value, value.getClass()
                                                               )));
        }
    }

}
