package org.mongodb.morphia.mapping.validation.classrules;


import java.util.List;
import java.util.Set;

import org.mongodb.morphia.annotations.Version;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.validation.ClassConstraint;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MultipleVersions implements ClassConstraint {

  public void check(final MappedClass mc, final Set<ConstraintViolation> ve) {
    final List<MappedField> versionFields = mc.getFieldsAnnotatedWith(Version.class);
    if (versionFields.size() > 1) {
      ve.add(new ConstraintViolation(Level.FATAL, mc, getClass(),
        "Multiple @" + Version.class + " annotations are not allowed. (" + new FieldEnumString(versionFields)));
    }
  }
}
