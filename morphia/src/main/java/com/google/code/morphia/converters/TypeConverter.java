/**
 * 
 */
package com.google.code.morphia.converters;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
@SuppressWarnings("unchecked")
public abstract class TypeConverter {
	abstract boolean canHandle(Class c, MappedField optionalExtraInfo);
	
	final boolean canHandle(Class c) {
		return canHandle(c, null);
	}
	
	abstract Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo)
			throws MappingException;
	
	final Object decode(Class targetClass, Object fromDBObject) throws MappingException {
		return decode(targetClass, fromDBObject, null);
	}
	
	final Object encode(Object value) throws MappingException {
		return encode(value, null);
	}
	
	boolean oneOf(Class f, Class... classes) {
		for (Class c : classes) {
			if (c.equals(f))
				return true;
		}
		return false;
	}
	
	Object encode(Object value, MappedField optionalExtraInfo) {
		return value; // as a default impl
	}
	
	final boolean canHandle(MappedField mf) {
		return canHandle(mf.getType(), mf);
	}
}
