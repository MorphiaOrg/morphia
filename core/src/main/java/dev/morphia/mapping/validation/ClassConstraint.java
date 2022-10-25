package dev.morphia.mapping.validation;

import java.util.Set;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;

/**
 * Defines a constraint for validation
 */
public interface ClassConstraint {
    /**
     * Check that an EntityModel meets the constraint
     *
     * @param model  the model to check
     * @param ve     the set of violations
     * @param mapper the Mapper to use for validation
     * @since 2.1
     */
    void check(Mapper mapper, EntityModel model, Set<ConstraintViolation> ve);
}
