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
public class EnumConverter extends TypeConverter {
	
	@Override
	boolean canHandle(Class c, MappedField optionalExtraInfo) {
		return c.isEnum();
	}
	
	@Override
	Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo) throws MappingException {
		return Enum.valueOf(targetClass, fromDBObject.toString());
	}
	
	@Override
	Object encode(Object value, MappedField optionalExtraInfo) {
		if (value == null)
			return null;
		
		return getName((Enum) value);
	}
	
	private <T extends Enum> String getName(T value) {
		return value.name();
	}
	
}
