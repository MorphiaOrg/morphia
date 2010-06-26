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
public class CharArrayConverter extends TypeConverter {
	@Override
	protected
	boolean isSupported(Class c, MappedField optionalExtraInfo) {
		return oneOf(c, char[].class);
	}
	
	@Override
	public
	Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo) throws MappingException {
		return fromDBObject.toString().toCharArray();
	}
	
	@Override
	public
	Object encode(Object value, MappedField optionalExtraInfo) {
		return new String((char[]) value);
	}
}
