package dev.morphia.mapping.validation.classrules;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Map;
import java.util.Set;

/**
 * Checks that a type is not a Map or Iterable subtype
 */
public class EntityCannotBeMapOrIterable implements ClassConstraint {
    @Override
    public void check(Mapper mapper, EntityModel entityModel, Set<ConstraintViolation> ve) {
        if (entityModel.getEntityAnnotation() != null && (Map.class.isAssignableFrom(entityModel.getType())
                                                          || Iterable.class.isAssignableFrom(entityModel.getType()))) {
            ve.add(new ConstraintViolation(Level.FATAL, entityModel, getClass(), "Entities cannot implement Map/Iterable"));
        }
    }
}
