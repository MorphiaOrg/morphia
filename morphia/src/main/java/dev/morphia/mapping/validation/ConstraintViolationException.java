package dev.morphia.mapping.validation;

import dev.morphia.mapping.MappingException;

import java.util.Collection;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ConstraintViolationException extends MappingException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a ConstraintViolationException with the set of violations
     *
     * @param ve the violations
     */
    public ConstraintViolationException(final Collection<ConstraintViolation> ve) {
        super(createString(ve.toArray(new ConstraintViolation[ve.size()])));
    }

    /**
     * Creates a ConstraintViolationException with the set of violations
     *
     * @param ve the violations
     */
    public ConstraintViolationException(final ConstraintViolation... ve) {
        super(createString(ve));
    }

    private static String createString(final ConstraintViolation[] ve) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Number of violations: " + ve.length + " \n");
        for (final ConstraintViolation validationError : ve) {
            sb.append(validationError.render());
        }
        return sb.toString();
    }
}
