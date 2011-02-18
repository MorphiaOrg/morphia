/**
 * 
 */
package com.google.code.morphia.mapping.validation.classrules;

import java.lang.reflect.Modifier;
import java.util.Set;

import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.validation.ClassConstraint;
import com.google.code.morphia.mapping.validation.ConstraintViolation;
import com.google.code.morphia.mapping.validation.ConstraintViolation.Level;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public class NonStaticInnerClasss implements ClassConstraint {

	public void check(MappedClass mc, Set<ConstraintViolation> ve) {
		Class<?> clazz = mc.getClazz();
		boolean isstatic = Modifier.isStatic(clazz.getModifiers());
		if (!isstatic && clazz.isMemberClass())
			ve.add(new ConstraintViolation(Level.FATAL, mc, this.getClass(), "Inner class is not static"));
	}
}
