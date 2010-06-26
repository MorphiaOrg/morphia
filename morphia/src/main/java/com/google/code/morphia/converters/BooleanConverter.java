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
public class BooleanConverter extends TypeConverter {
	@Override
	protected
	boolean isSupported(Class c, MappedField optionalExtraInfo) {
		return oneOf(c, boolean.class, Boolean.class);
	}
	
	@Override
	public
	Object decode(Class targetClass, Object val, MappedField optionalExtraInfo) throws MappingException {
		Object dbValue = val;
		if (dbValue instanceof Boolean) {
			return (Boolean) val;
		}
		String sVal = val.toString();
		return Boolean.parseBoolean(sVal);
	}
	
}
