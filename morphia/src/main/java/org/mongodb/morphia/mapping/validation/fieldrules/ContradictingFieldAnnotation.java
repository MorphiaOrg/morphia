package org.mongodb.morphia.mapping.validation.fieldrules;


import java.lang.annotation.Annotation;
import java.util.Set;

import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ContradictingFieldAnnotation extends FieldConstraint {

  private final Class<? extends Annotation> a1;
  private final Class<? extends Annotation> a2;

  public ContradictingFieldAnnotation(final Class<? extends Annotation> a1, final Class<? extends Annotation> a2) {
    this.a1 = a1;
    this.a2 = a2;
  }

  @Override
  protected final void check(final MappedClass mc, final MappedField mf, final Set<ConstraintViolation> ve) {
    if (mf.hasAnnotation(a1) && mf.hasAnnotation(a2)) {
      ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(),
        "A field can be either annotated with @" + a1.getSimpleName() + " OR @" + a2.getSimpleName() + ", but not both."));
    }
  }
}
