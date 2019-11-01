package dev.morphia.query.validation;

import java.util.List;

/**
 * Provides validation based on the Value in the query
 */
public abstract class ValueValidator implements Validator {
    /**
     * Applied validation for the given field.  If the value does not match the correct type, the validation is not applied and this method
     * returns false.  If the value is to be validated, then the validate method is called to see if the value and type are compatible.
     * Errors are appended to the validationFailures list.
     *
     * @param type               the Class of the field being queried
     * @param value              the non-null value being used for a query
     * @param validationFailures the list to add any failures to. If validation passes or {@code appliesTo} returned false, this list will
     *                           not change.
     * @return true if validation was applied, false if this validation doesn't apply to this field type.
     */
    public boolean apply(final Class<?> type, final Object value, final List<ValidationFailure> validationFailures) {
        if (getRequiredValueType().isAssignableFrom(value.getClass())) {
            validate(type, value, validationFailures);
            return true;
        }
        return false;
    }

    /**
     * Used by {@code apply} to figure out whether to apply the validation or simply return.
     *
     * @return the class the value should be in order to go ahead and perform validation
     */
    protected abstract Class<?> getRequiredValueType();

    protected abstract void validate(Class<?> type, Object value, List<ValidationFailure> validationFailures);
}
