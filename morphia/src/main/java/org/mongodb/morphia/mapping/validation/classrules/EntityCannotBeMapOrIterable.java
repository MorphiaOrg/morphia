package org.mongodb.morphia.mapping.validation.classrules;


import java.util.Map;
import java.util.Set;

import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.validation.ClassConstraint;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class EntityCannotBeMapOrIterable implements ClassConstraint {

  public void check(final MappedClass mc, final Set<ConstraintViolation> ve) {

    if (mc.getEntityAnnotation() != null && (Map.class.isAssignableFrom(mc.getClazz()) || Iterable.class.isAssignableFrom(mc.getClazz()))) {
      ve.add(new ConstraintViolation(Level.FATAL, mc, getClass(), "Entities cannot implement Map/Iterable"));
    }

  }
}
