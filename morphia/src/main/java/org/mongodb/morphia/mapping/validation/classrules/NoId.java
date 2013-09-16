package org.mongodb.morphia.mapping.validation.classrules;


import java.util.Set;

import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.validation.ClassConstraint;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class NoId implements ClassConstraint {

  public void check(final MappedClass mc, final Set<ConstraintViolation> ve) {
    if (mc.getIdField() == null && mc.getEmbeddedAnnotation() == null) {
      ve.add(new ConstraintViolation(Level.FATAL, mc, getClass(), "No field is annotated with @Id; but it is required"));
    }
  }

}
