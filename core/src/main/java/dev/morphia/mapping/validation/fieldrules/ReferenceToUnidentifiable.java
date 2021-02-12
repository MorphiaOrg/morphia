package dev.morphia.mapping.validation.fieldrules;

import dev.morphia.Key;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.NotMappableException;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.sofia.Sofia;

import java.util.Set;

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
                try {
                    mapper.getEntityModel(realType);
                } catch (NotMappableException ignored) {
                    ve.add(new ConstraintViolation(Level.FATAL, entityModel, propertyModel, getClass(),
                        Sofia.referredTypeMissingId(propertyModel.getFullName(), propertyModel.getType().getName())));
                }
            }
        }
    }

}
