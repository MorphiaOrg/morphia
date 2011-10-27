/**
 * 
 */
package com.google.code.morphia.converters;

import java.util.ArrayList;
import java.util.List;

import org.bson.LazyBSONList;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.utils.ReflectionUtils;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({"rawtypes"})
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

		//FixMe: super-hacky
		if (val instanceof LazyBSONList || val instanceof ArrayList)
			return ReflectionUtils.convertToArray(double.class, (List<?>)val);
			
		String sVal = val.toString();
		return Double.parseDouble(sVal);
	}
}
