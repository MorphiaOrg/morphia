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
public class StringConverter extends TypeConverter {
	public StringConverter() {
		super(String.class);
	}
	@Override
	public Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo) throws MappingException {
		if (fromDBObject instanceof String) {
			return (String) fromDBObject;
		}
		return fromDBObject.toString();
	}
	
}
