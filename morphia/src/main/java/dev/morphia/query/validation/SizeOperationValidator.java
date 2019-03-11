package dev.morphia.query.validation;

import dev.morphia.mapping.MappedField;
import dev.morphia.query.FilterOperator;

import java.util.List;

import static java.lang.String.format;
import static dev.morphia.query.FilterOperator.SIZE;
import static dev.morphia.query.validation.ValueClassValidator.valueIsClassOrSubclassOf;

/**
 * Checks if the value can have the {@code FilterOperator.ALL} operator applied to it.  Since this class does not need state, and the
 * methods can't be static because it implements an interface, it seems to be one of the few places where the Singleton pattern seems
 * appropriate.
 */
public final class SizeOperationValidator extends OperationValidator {
    private static final SizeOperationValidator INSTANCE = new SizeOperationValidator();

    private SizeOperationValidator() {
    }

    /**
     * Get the instance
     *
     * @return the Singleton instance of this validator
     */
    public static SizeOperationValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected FilterOperator getOperator() {
        return SIZE;
    }

    @Override
    protected void validate(final MappedField mappedField, final Object value,
                            final List<ValidationFailure> validationFailures) {
        if (!valueIsClassOrSubclassOf(value, Number.class)) {
            validationFailures.add(new ValidationFailure(format("For a $size operation, value '%s' should be an integer type.  "
                                                                + "Instead it was a: %s", value, value.getClass())));

        }
        if (!CollectionTypeValidator.typeIsIterableOrArrayOrMap(mappedField.getType())) {
            validationFailures.add(new ValidationFailure(format("For a $size operation, field '%s' should be a List or array.  "
                                                                + "Instead it was a: %s",
                                                                mappedField, mappedField.getType())));
        }
    }
}
