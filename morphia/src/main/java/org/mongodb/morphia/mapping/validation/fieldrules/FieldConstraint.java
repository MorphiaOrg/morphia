package org.mongodb.morphia.mapping.validation.fieldrules;


import java.util.Set;

import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.validation.ClassConstraint;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public abstract class FieldConstraint implements ClassConstraint {
  Mapper mapper;

  public final void check(final MappedClass mc, final Set<ConstraintViolation> ve) {
    for (final MappedField mf : mc.getPersistenceFields()) {
      check(mc, mf, ve);
    }
  }

  protected abstract void check(MappedClass mc, MappedField mf, Set<ConstraintViolation> ve);

}
