package dev.morphia.mapping.validation.fieldrules;

import java.util.Set;

import dev.morphia.Key;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.sofia.Sofia;

/**
 * Checks that references point to mapped types.
 */
public class ReferenceToUnidentifiable extends PropertyConstraint {

    @Override
    protected void check(Mapper mapper, EntityModel entityModel, PropertyModel propertyModel, Set<ConstraintViolation> ve) {
        if (propertyModel.hasAnnotation(Reference.class)) {
            final Class realType = propertyModel.getNormalizedType();

            if (realType.equals(Key.class)) {
                ve.add(new ConstraintViolation(Level.FATAL, entityModel, propertyModel, getClass(), Sofia.keyNotAllowedAsProperty()));
            } else {
                if (!mapper.tryGetEntityModel(realType).isPresent()) {
                    ve.add(new ConstraintViolation(Level.FATAL, entityModel, propertyModel, getClass(),
                            Sofia.referredTypeMissingId(propertyModel.getFullName(), propertyModel.getType().getName())));
                }
            }
        }
    }

}
