/**
 * 
 */
package com.google.code.morphia.converters;

import org.bson.types.ObjectId;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
@SuppressWarnings("unchecked")
public class ObjectIdConverter extends TypeConverter {
	@Override
	boolean canHandle(Class c, MappedField optionalExtraInfo) {
		return oneOf(c, ObjectId.class);
	}
	
	@Override
	Object decode(Class targetClass, Object val, MappedField optionalExtraInfo) throws MappingException {
		return val;
	}
	
}
