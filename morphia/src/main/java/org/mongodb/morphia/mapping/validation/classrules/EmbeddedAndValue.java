package org.mongodb.morphia.mapping.validation.classrules;


import java.util.Set;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.validation.ClassConstraint;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class EmbeddedAndValue implements ClassConstraint {

  public void check(final MappedClass mc, final Set<ConstraintViolation> ve) {

    if (mc.getEmbeddedAnnotation() != null && !mc.getEmbeddedAnnotation().value().equals(Mapper.IGNORED_FIELDNAME)) {
      ve.add(new ConstraintViolation(Level.FATAL, mc, getClass(),
        "@" + Embedded.class.getSimpleName() + " classes cannot specify a fieldName value(); this is on applicable on fields"));
    }
  }

}
