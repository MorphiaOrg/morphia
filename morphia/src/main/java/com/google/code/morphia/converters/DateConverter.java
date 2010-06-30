/**
 * 
 */
package com.google.code.morphia.converters;

import java.util.Date;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
@SuppressWarnings("unchecked")
public class DateConverter extends TypeConverter {
	@Override
	protected
	boolean isSupported(Class c, MappedField optionalExtraInfo) {
		return oneOf(c, Date.class);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public
	Object decode(Class targetClass, Object o, MappedField optionalExtraInfo) throws MappingException {
		if (o instanceof Date) {
			Date d = (Date) o;
			return d;
		}
		return new Date(Date.parse(o.toString())); // good luck
	}
	
	public boolean isSimpleValue() {
		return true;
	}

}
