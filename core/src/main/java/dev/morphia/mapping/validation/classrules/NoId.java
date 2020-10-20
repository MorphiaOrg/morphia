package dev.morphia.mapping.validation.classrules;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.sofia.Sofia;

import java.util.Set;

/**
 * Validates that classes marked with @Entity have a field annotated with @Id.
 */
public class NoId implements ClassConstraint {
    @Override
    public void check(Mapper mapper, EntityModel entityModel, Set<ConstraintViolation> ve) {
        if (entityModel.getIdField() == null && entityModel.getEntityAnnotation() != null) {
            ve.add(new ConstraintViolation(Level.FATAL, entityModel, getClass(), Sofia.noIdFieldFound(entityModel.getType().getName())));
        }
    }
}
