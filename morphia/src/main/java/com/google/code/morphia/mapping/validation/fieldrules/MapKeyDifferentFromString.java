/**
 * 
 */
package com.google.code.morphia.mapping.validation.fieldrules;

import java.util.Set;

import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.validation.ConstraintViolation;
import com.google.code.morphia.mapping.validation.ConstraintViolation.Level;
import com.google.code.morphia.utils.ReflectionUtils;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
@SuppressWarnings("unchecked")
public class MapKeyDifferentFromString extends FieldConstraint {
	@Override
	protected void check(MappedClass mc, MappedField mf, Set<ConstraintViolation> ve) {
		if (mf.isMap() && (!mf.hasAnnotation(Serialized.class))) {
			Class parameterizedClass = ReflectionUtils.getParameterizedClass(mf.getField(), 0);
			if (parameterizedClass == null) {
				ve.add(new ConstraintViolation(Level.WARNING, mc, mf,
						"Maps must be keyed by type String (Map<String,?>). Use parametrized types if possible."));
			} else if (!parameterizedClass.equals(String.class))
				ve
						.add(new ConstraintViolation(Level.FATAL, mc, mf,
								"Maps must be keyed by type String (Map<String,?>)"));
		}
	}
}
