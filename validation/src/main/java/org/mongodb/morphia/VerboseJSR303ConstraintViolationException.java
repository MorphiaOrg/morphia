package org.mongodb.morphia;


import org.mongodb.morphia.utils.Assert;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;


/**
 * @author us@thomas-daily.de
 */
public class VerboseJSR303ConstraintViolationException extends ConstraintViolationException {
    /**
     * Creates a VerboseJSR303ConstraintViolationException
     * @param vio the violations
     */
    public VerboseJSR303ConstraintViolationException(final Set<ConstraintViolation<?>> vio) {
        super(createVerboseMessage(vio), vio);
        Assert.parameterNotNull("vio", vio);
    }

    private static String createVerboseMessage(final Set<ConstraintViolation<?>> vio) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("The following constraints have been violated:\n");
        for (final ConstraintViolation<?> c : vio) {
            sb.append(c.getRootBeanClass().getSimpleName());
            sb.append(".");
            sb.append(c.getPropertyPath());
            sb.append(": ");
            sb.append(c.getMessage());
            sb.append(" ('");
            sb.append(c.getInvalidValue());
            sb.append("')\n");
        }
        return sb.toString();
    }
}
