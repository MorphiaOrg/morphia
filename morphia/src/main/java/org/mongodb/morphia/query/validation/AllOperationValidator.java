package org.mongodb.morphia.query.validation;

import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.query.FilterOperator;

import java.util.List;

import static java.lang.String.format;
import static org.mongodb.morphia.query.FilterOperator.ALL;
import static org.mongodb.morphia.query.validation.CollectionTypeValidator.typeIsIterableOrArrayOrMap;

/**
 * Validates a query that uses the FilterOperator.ALL operator.
 */
public final class AllOperationValidator extends OperationValidator {
    private static final AllOperationValidator INSTANCE = new AllOperationValidator();

    private AllOperationValidator() {
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

    @Override
    protected FilterOperator getOperator() {
        return ALL;
    }

    /**
     * Get the instance.
     *
     * @return the Singleton instance of this validator
     */
    public static AllOperationValidator getInstance() {
        return INSTANCE;
    }

}
