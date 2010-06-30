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
@SuppressWarnings({"unchecked","rawtypes"})
public class ClassConverter extends TypeConverter {

	public ClassConverter() { super(Class.class); }

	@Override
	public Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo) throws MappingException {
		if (fromDBObject == null)
        {
            return null;
        }

		String l = fromDBObject.toString();
		try
        {
            return Class.forName(l);
        }
        catch (ClassNotFoundException e)
        {
            throw new MappingException("Cannot creat class from Name '"+l+"'",e);
        }
	}

	@Override
	public Object encode(Object value, MappedField optionalExtraInfo) {
		if (value == null)
        {
            return null;
        }
		return ((Class)value).getName();
	}
	
	@Override
	public boolean isSimpleValue() {
		return true;
	}
}
