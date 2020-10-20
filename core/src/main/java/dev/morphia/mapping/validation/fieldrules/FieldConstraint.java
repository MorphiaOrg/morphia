package dev.morphia.mapping.validation.fieldrules;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;

import java.util.Set;

/**
 * Defines a field constraint.
 */
public abstract class FieldConstraint implements ClassConstraint {
    @Override
    public final void check(Mapper mapper, EntityModel entityModel, Set<ConstraintViolation> ve) {
        for (FieldModel mf : entityModel.getFields()) {
            check(mapper, entityModel, mf, ve);
        }
    }

    protected abstract void check(Mapper mapper, EntityModel entityModel, FieldModel mf, Set<ConstraintViolation> ve);

}
