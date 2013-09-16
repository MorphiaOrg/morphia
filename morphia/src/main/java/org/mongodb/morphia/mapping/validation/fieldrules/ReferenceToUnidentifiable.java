package org.mongodb.morphia.mapping.validation.fieldrules;


import java.util.Set;

import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.MappingException;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolation.Level;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ReferenceToUnidentifiable extends FieldConstraint {

  @Override
  protected void check(final MappedClass mc, final MappedField mf, final Set<ConstraintViolation> ve) {
    if (mf.hasAnnotation(Reference.class)) {
      final Class realType = (mf.isSingleValue()) ? mf.getType() : mf.getSubClass();

      if (realType == null) {
        throw new MappingException("Type is null for this MappedField: " + mf);
      }

      if ((!realType.isInterface() && mc.getMapper().getMappedClass(realType).getIdField() == null)) {
        ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(),
          mf.getFullName() + " is annotated as a @" + Reference.class.getSimpleName() + " but the " + mf.getType().getName()
            + " class is missing the @" + Id.class.getSimpleName() + " annotation"));
      }
    }
  }

}
