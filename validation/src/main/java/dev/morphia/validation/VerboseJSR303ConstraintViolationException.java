package dev.morphia.validation;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import static java.lang.String.format;

/**
 * Exception for validation failures
 */
public class VerboseJSR303ConstraintViolationException extends ConstraintViolationException {
    /**
     * Creates a VerboseJSR303ConstraintViolationException
     *
     * @param vio the violations
     */
    public VerboseJSR303ConstraintViolationException(Set<ConstraintViolation<?>> vio) {
        super(createVerboseMessage(vio), vio);
    }

    private static String createVerboseMessage(Set<ConstraintViolation<?>> vio) {
        return vio.stream().map(c -> format("%s.%s:%s ('%s')",
                c.getRootBeanClass().getSimpleName(),
                c.getPropertyPath(),
                c.getMessage(),
                c.getInvalidValue()))
                .collect(Collectors.joining("\n", "The following constraints have been violated:\n", ""));
    }
}
