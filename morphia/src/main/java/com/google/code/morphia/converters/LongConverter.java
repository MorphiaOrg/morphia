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
public class LongConverter extends TypeConverter {
	
	public LongConverter() {
		super(long.class, Long.class);
	}
	
	@Override
	public Object decode(Class targetClass, Object val, MappedField optionalExtraInfo) throws MappingException {
		if (val instanceof String) {
			return Long.parseLong((String) val);
		} else {
			return ((Number) val).longValue();
		}
		
	}
	
}
