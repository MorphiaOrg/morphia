package dev.morphia.mapping.validation.fieldrules;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;

import java.util.Set;

/**
 * Defines a property constraint.
 */
public abstract class PropertyConstraint implements ClassConstraint {
    @Override
    public final void check(Mapper mapper, EntityModel entityModel, Set<ConstraintViolation> ve) {
        for (PropertyModel model : entityModel.getProperties()) {
            check(mapper, entityModel, model, ve);
        }
    }

    protected abstract void check(Mapper mapper, EntityModel entityModel, PropertyModel propertyModel, Set<ConstraintViolation> ve);

}
