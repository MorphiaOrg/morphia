package dev.morphia.mapping.validation.fieldrules;

import java.util.Set;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;

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

    /**
     * Checks one property, so a validator can run every property rule in a single pass over the properties.
     *
     * @param mapper        the mapper
     * @param entityModel   the model the property belongs to
     * @param propertyModel the property to check
     * @param ve            the violations found so far, to add to
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public final void checkProperty(Mapper mapper, EntityModel entityModel, PropertyModel propertyModel,
            Set<ConstraintViolation> ve) {
        check(mapper, entityModel, propertyModel, ve);
    }

    protected abstract void check(Mapper mapper, EntityModel entityModel, PropertyModel propertyModel, Set<ConstraintViolation> ve);
}
