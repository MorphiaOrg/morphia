package dev.morphia.query.validation;

import java.util.List;

/**
 * Validates Lists.  Currently a noop.
 */
// TODO: Trisha - really not sure why lists are always valid values. This should be a real Type validator which checks against the field,
// if in fact that's not already done by another validator
public final class ListValueValidator extends ValueValidator {
    private static final ListValueValidator INSTANCE = new ListValueValidator();

    private ListValueValidator() {
    }

    /**
     * Get the instance.
     *
     * @return the Singleton instance of this validator
     */
    public static ListValueValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected Class getRequiredValueType() {
        return List.class;
    }

    @Override
    protected void validate(final Class<?> type, final Object value, final List<ValidationFailure> validationFailures) {
        //preserving current behaviour - this never fails validation
    }
}
