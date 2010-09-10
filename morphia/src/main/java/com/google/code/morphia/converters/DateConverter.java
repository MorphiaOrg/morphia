/**
 * 
 */
package com.google.code.morphia.converters;

import java.util.Date;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class DateConverter extends TypeConverter implements SimpleValueConverter{
	
	public DateConverter() { super(Date.class); };
	
	@SuppressWarnings("deprecation")
	@Override
	public
	Object decode(Class targetClass, Object val, MappedField optionalExtraInfo) throws MappingException {
		if (val == null) return null;

		if (val instanceof Date)
			return val;
			
		return new Date(Date.parse(val.toString())); // good luck
	}
}
