package dev.morphia.mapping.validation;

import java.util.Collection;

import dev.morphia.mapping.MappingException;

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
    public ConstraintViolationException(Collection<ConstraintViolation> ve) {
        super(createString(ve.toArray(new ConstraintViolation[ve.size()])));
    }

    /**
     * Creates a ConstraintViolationException with the set of violations
     *
     * @param ve the violations
     */
    public ConstraintViolationException(ConstraintViolation... ve) {
        super(createString(ve));
    }

    private static String createString(ConstraintViolation[] ve) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Number of violations: " + ve.length + " \n");
        for (ConstraintViolation validationError : ve) {
            sb.append(validationError.render());
        }
        return sb.toString();
    }
}
