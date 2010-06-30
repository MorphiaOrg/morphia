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
public class ByteConverter extends TypeConverter {
	@Override
	protected
	boolean isSupported(Class c, MappedField optionalExtraInfo) {
		return oneOf(c, Byte.class, byte.class);
	}
	
	@Override
	public
	Object decode(Class targetClass, Object val, MappedField optionalExtraInfo) throws MappingException {
		Object dbValue = val;
		if (dbValue instanceof Double) {
			return ((Double) dbValue).byteValue();
		} else if (dbValue instanceof Integer) {
			return ((Integer) dbValue).byteValue();
		}
		String sVal = val.toString();
		return Byte.parseByte(sVal);
	}
	
	public boolean isSimpleValue() {
		return true;
	}

}
