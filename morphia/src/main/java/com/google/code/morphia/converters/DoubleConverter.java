/**
 * 
 */
package com.google.code.morphia.converters;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class DoubleConverter extends TypeConverter implements SimpleValueConverter{

	public DoubleConverter() { super(double.class, Double.class); }
	
	@Override
	public
	Object decode(Class targetClass, Object val, MappedField optionalExtraInfo) throws MappingException {
		if (val == null) return null;
		
		if (val instanceof Double)
			return (Double) val;
		
		if (val instanceof Number)
			return ((Number) val).doubleValue();

		String sVal = val.toString();
		return Double.parseDouble(sVal);
	}
}
