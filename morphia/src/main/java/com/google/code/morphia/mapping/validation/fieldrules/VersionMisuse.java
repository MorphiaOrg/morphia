/**
 * 
 */
package com.google.code.morphia.mapping.validation.fieldrules;

import java.util.Set;

import com.google.code.morphia.annotations.Version;
import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.validation.ConstraintViolation;
import com.google.code.morphia.mapping.validation.ConstraintViolation.Level;
import com.google.code.morphia.utils.ReflectionUtils;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class VersionMisuse extends FieldConstraint {
	
	@Override
	protected void check(MappedClass mc, MappedField mf, Set<ConstraintViolation> ve) {
		if (mf.hasAnnotation(Version.class)) {
			Class<?> type = mf.getType();
			if (Long.class.equals(type) || long.class.equals(type)) {
				

				// hope that this is tested already by now?
					Object testInstance = ReflectionUtils.createInstance(mc.getClazz());

				// check initial value
				if (Long.class.equals(type)) {
					if (mf.getFieldValue(testInstance) != null)
						ve.add(new ConstraintViolation(Level.FATAL, mc, mf, "When using @"
								+ Version.class.getSimpleName() + " on a Long field, it has to be null initially."));
				}
				
				if (long.class.equals(type)) {
						if ((Long) mf.getFieldValue(testInstance) != 0L)
							ve.add(new ConstraintViolation(Level.FATAL, mc, mf, "When using @"
									+ Version.class.getSimpleName() + " on a long field, it has to be 0 initially."));
						
					}
					


			} else
				ve.add(new ConstraintViolation(Level.FATAL, mc, mf, "@" + Version.class.getSimpleName()
						+ " can only be used on a field typed Long or long."));
		}
	}
	
}
