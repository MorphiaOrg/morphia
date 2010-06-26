/**
 * 
 */
package com.google.code.morphia.converters;

import com.google.code.morphia.Key;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.mongodb.DBRef;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
@SuppressWarnings("unchecked")
public class KeyConverter extends TypeConverter {

	public KeyConverter() { super(Key.class); }
	
	@Override
	public Object decode(Class targetClass, Object o, MappedField optionalExtraInfo) throws MappingException {
		return new Key((DBRef) o);
	}
	
	@Override
	public
	Object encode(Object t, MappedField optionalExtraInfo) {
		if (t == null)
			return null;
		return ((Key) t).toRef(mapr);
	}
	
}
