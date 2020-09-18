package dev.morphia.mapping.validation.classrules;

import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
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
    public void check(final Mapper mapper, final MappedClass mc, final Set<ConstraintViolation> ve) {
        if (mc.getIdField() == null && mc.getEntityAnnotation() != null) {
            ve.add(new ConstraintViolation(Level.FATAL, mc, getClass(), Sofia.noIdFieldFound(mc.getType().getName())));
        }
    }
}
