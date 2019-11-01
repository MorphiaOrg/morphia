package dev.morphia.query.validation;

import java.util.List;

/**
 * Extend this class to provide specific validation for field types for query validation.
 */
public abstract class TypeValidator implements Validator {
    /**
     * Apply validation for the given field.  If the field is not of a type in the list returned by getTypeClasses, the validation is not
     * applied and this method returns false.  If the type is in the list, then the validate method is called to see if the value is of a
     * type that can be applied to the given field type.  Errors are appended to the validationFailures list.
     *
     * @param type               the Class of the field being queried
     * @param value              the value being used for a query
     * @param validationFailures the list to add any failures to. If validation passes or {@code appliesTo} returned false, this list will
     *                           not change.
     * @return true if validation was applied, false if this validation doesn't apply to this field type.
     */
    public boolean apply(final Class<?> type, final Object value,
                         final List<ValidationFailure> validationFailures) {

        if (appliesTo(type)) {
            validate(type, value, validationFailures);
            return true;
        }
        return false;
    }

    /**
     * Used by apply to figure out whether to apply the validation or simply return.
     *
     * @param type the type to be validated
     * @return true if this validator applies to this type, false otherwise
     */
    protected abstract boolean appliesTo(Class<?> type);

    protected abstract void validate(Class<?> type, Object value, List<ValidationFailure> validationFailures);

}
