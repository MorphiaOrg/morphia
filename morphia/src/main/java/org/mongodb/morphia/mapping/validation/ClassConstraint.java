package org.mongodb.morphia.mapping.validation;


import java.util.Set;

import org.mongodb.morphia.mapping.MappedClass;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public interface ClassConstraint {
  void check(MappedClass mc, Set<ConstraintViolation> ve);
}
