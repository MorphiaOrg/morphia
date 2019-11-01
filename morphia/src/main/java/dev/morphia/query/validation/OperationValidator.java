package dev.morphia.query.validation;

import dev.morphia.mapping.MappedField;
import dev.morphia.query.FilterOperator;

import java.util.List;

/**
 * Extend this abstract class to provide a way of validating part of a query that contains a {@code FilterOperator}. Currently all
 * subclasses of this are final and singletons so this isn't the root of a massive class hierarchy.
 */
public abstract class OperationValidator implements Validator {
    /**
     * Apply validation for the given operator.  If the operator does not match the operator required by the implementing class, then this
     * method will return false to show validation was not applied.  If the operator is the one being validated, this method will return
     * true, and any failures in validation will be added to the list of {@code validationFailures}.
     *
     * @param mappedField        the field being queried
     * @param operator           any FilterOperator for a query
     * @param value              the query value, to apply the operator to
     * @param validationFailures the list to add any failures to. If validation passes or {@code appliesTo} returned false, this list will
     *                           not change.
     * @return true if validation was applied, false if this validation doesn't apply to this operator.
     */
    public boolean apply(final MappedField mappedField, final FilterOperator operator, final Object value,
                         final List<ValidationFailure> validationFailures) {
        if (getOperator().equals(operator)) {
            validate(mappedField, value, validationFailures);
            return true;
        }
        return false;
    }

    /**
     * This method is called by the {@code apply} method to determine whether to validate the query.  The validator will only work for a
     * single FilterOperator, and this will be returned by this method
     *
     * @return the FilterOperator this validator cares about.
     */
    protected abstract FilterOperator getOperator();

    /**
     * Performs the actual validation, and assumes {@code appliesTo} has returned true
     *
     * @param mappedField        the field being queried
     * @param value              the query value, to apply the operator to. This should not be null.
     * @param validationFailures the list to add any new {@code ValidationFailures} to. If validation passed this list will not change.
     */
    protected abstract void validate(MappedField mappedField, Object value, List<ValidationFailure> validationFailures);

}
