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
public class IntegerConverter extends TypeConverter {
	public IntegerConverter() {
		super(int.class, Integer.class);
	}
	
	@Override
	public Object decode(Class targetClass, Object val, MappedField optionalExtraInfo) throws MappingException {
		if (val instanceof String) {
			return Integer.parseInt((String) val);
		} else {
			return ((Number) val).intValue();
		}
		
	}
	
	public boolean isSimpleValue() {
		return true;
	}

}
