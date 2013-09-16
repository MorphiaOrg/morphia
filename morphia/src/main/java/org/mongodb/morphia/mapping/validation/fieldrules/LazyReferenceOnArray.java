package org.mongodb.morphia.mapping.validation.fieldrules;


import java.util.Set;

import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class LazyReferenceOnArray extends FieldConstraint {

  @Override
  protected void check(final MappedClass mc, final MappedField mf, final Set<ConstraintViolation> ve) {
    final Reference ref = mf.getAnnotation(Reference.class);
    if (ref != null && ref.lazy()) {
      final Class type = mf.getType();
      if (type.isArray()) {
        ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(),
          "The lazy attribute cannot be used for an Array. If you need a lazy array please use ArrayList instead."));
      }
    }
  }

}
