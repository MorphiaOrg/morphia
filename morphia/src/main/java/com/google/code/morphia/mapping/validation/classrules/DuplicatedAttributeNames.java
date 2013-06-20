package com.google.code.morphia.mapping.validation.classrules;


import java.util.HashSet;
import java.util.Set;

import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.validation.ClassConstraint;
import com.google.code.morphia.mapping.validation.ConstraintViolation;
import com.google.code.morphia.mapping.validation.ConstraintViolation.Level;


/**
 * @author josephpachod
 */
public class DuplicatedAttributeNames implements ClassConstraint {

  public void check(final MappedClass mc, final Set<ConstraintViolation> ve) {
    final Set<String> foundNames = new HashSet<String>();
    for (final MappedField mappedField : mc.getPersistenceFields()) {
      for (final String name : mappedField.getLoadNames()) {
        if (!foundNames.add(name)) {
          ve.add(new ConstraintViolation(Level.FATAL, mc, mappedField, getClass(),
            "Mapping to MongoDB field name '" + name + "' is duplicated; you cannot map different java fields to the same MongoDB field."));
        }
      }
    }
  }
}
