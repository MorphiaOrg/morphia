package dev.morphia.mapping.validation.classrules;

import java.util.Set;

import dev.morphia.annotations.ExternalEntity;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import static dev.morphia.sofia.Sofia.mappingAnnotationNeeded;

/**
 * Checks that @Entity or @ExternalEntity are used on a type.
 */
public class EntityOrExternal implements ClassConstraint {

    @Override
    public void check(Mapper mapper, EntityModel entityModel, Set<ConstraintViolation> ve) {
        if (entityModel.getEntityAnnotation() == null
                && entityModel.getAnnotation(ExternalEntity.class) == null) {
            ve.add(new ConstraintViolation(Level.FATAL, entityModel, getClass(), mappingAnnotationNeeded(entityModel.getType().getName())));
        }
    }
}
