package org.mongodb.morphia.mapping.validation.classrules;


import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.validation.ClassConstraint;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Set;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class NoId implements ClassConstraint {

    @Override
    public void check(final MappedClass mc, final Set<ConstraintViolation> ve) {
        if (mc.getIdField() == null && mc.getEmbeddedAnnotation() == null) {
            ve.add(new ConstraintViolation(Level.FATAL, mc, getClass(), "No field is annotated with @Id; but it is required"));
        }
    }

}
