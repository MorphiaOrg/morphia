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
public class DoubleConverter extends TypeConverter {
	
	@Override
	protected
	boolean isSupported(Class c, MappedField optionalExtraInfo) {
		return oneOf(c, double.class, Double.class);
	}
	
	@Override
	public
	Object decode(Class targetClass, Object val, MappedField optionalExtraInfo) throws MappingException {
		if (val instanceof Double) {
			return (Double) val;
		}
		Object dbValue = val;
		if (dbValue instanceof Number) {
			return ((Number) dbValue).doubleValue();
		}
		String sVal = val.toString();
		return Double.parseDouble(sVal);
	}
	
}
