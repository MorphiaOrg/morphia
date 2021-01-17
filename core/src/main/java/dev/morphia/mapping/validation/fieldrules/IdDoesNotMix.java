package dev.morphia.mapping.validation.fieldrules;


import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.sofia.Sofia;

import java.util.Set;

/**
 * Checks that @Id is not with any other mapping annotation
 */
public class IdDoesNotMix extends PropertyConstraint {

    @Override
    protected void check(Mapper mapper, EntityModel entityModel, PropertyModel propertyModel, Set<ConstraintViolation> ve) {
        // an @Id field can not be a Value, Reference, or Embedded
        if (propertyModel.hasAnnotation(Id.class)
            && (propertyModel.hasAnnotation(Reference.class)
                || propertyModel.hasAnnotation(Embedded.class)
                || propertyModel.hasAnnotation(Property.class))) {
            ve.add(new ConstraintViolation(Level.FATAL, entityModel, propertyModel, getClass(),
                Sofia.invalidAnnotationCombination(propertyModel.getFullName(), Id.class.getSimpleName())));
        }
    }
}
