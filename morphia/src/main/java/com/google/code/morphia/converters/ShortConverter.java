/**
 * 
 */
package com.google.code.morphia.converters;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
@SuppressWarnings("unchecked")
public class ShortConverter extends TypeConverter {
	@Override
	boolean canHandle(Class c, MappedField optionalExtraInfo) {
		return oneOf(c, short.class, Short.class);
	}
	
	@Override
	Object decode(Class targetClass, Object val, MappedField optionalExtraInfo) throws MappingException {
		Object dbValue = val;
		if (dbValue instanceof Double) {
			return ((Double) dbValue).shortValue();
		} else if (dbValue instanceof Integer) {
			return ((Integer) dbValue).shortValue();
		}
		String sVal = val.toString();
		return Short.parseShort(sVal);
	}
	
}
