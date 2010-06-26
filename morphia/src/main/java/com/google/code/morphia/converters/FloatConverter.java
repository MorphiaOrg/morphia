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
public class FloatConverter extends TypeConverter {
	
	public FloatConverter() { super(Float.class, float.class); }
	
	@Override
	public Object decode(Class targetClass, Object val, MappedField optionalExtraInfo) throws MappingException {
		Object dbValue = val;
		if (dbValue instanceof Double) {
			return ((Double) dbValue).floatValue();
		}
		String sVal = val.toString();
		return Float.parseFloat(sVal);
	}
}
