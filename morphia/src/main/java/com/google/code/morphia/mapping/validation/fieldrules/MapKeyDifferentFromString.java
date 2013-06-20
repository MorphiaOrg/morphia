package com.google.code.morphia.mapping.validation.fieldrules;


import java.util.Set;

import org.bson.types.ObjectId;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.validation.ConstraintViolation;
import com.google.code.morphia.mapping.validation.ConstraintViolation.Level;
import com.google.code.morphia.utils.ReflectionUtils;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MapKeyDifferentFromString extends FieldConstraint {
  private static final String supportedExample = "(Map<String/Enum/Long/ObjectId/..., ?>)";

  @Override
  protected void check(final MappedClass mc, final MappedField mf, final Set<ConstraintViolation> ve) {
    if (mf.isMap() && (!mf.hasAnnotation(Serialized.class))) {
      final Class<?> aClass = ReflectionUtils.getParameterizedClass(mf.getField(), 0);
      if (aClass == null) {
        ve.add(new ConstraintViolation(Level.WARNING, mc, mf, getClass(),
          "Maps cannot be keyed by Object (Map<Object,?>); Use a parametrized type that is supported " + supportedExample));
      } else if (!aClass.equals(String.class) && !aClass.equals(ObjectId.class) && !ReflectionUtils.isPrimitiveLike(
        aClass)) {
        ve.add(new ConstraintViolation(Level.FATAL, mc, mf, getClass(),
          "Maps must be keyed by a simple type " + supportedExample + "; " + aClass + " is not supported as a map key type."));
      }
    }
  }
}
