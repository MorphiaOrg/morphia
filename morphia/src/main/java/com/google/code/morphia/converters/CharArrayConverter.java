package com.google.code.morphia.converters;


import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 * @deprecated replaced by CharacterConverter
 */
@SuppressWarnings({"rawtypes" })
@Deprecated
public class CharArrayConverter extends TypeConverter implements SimpleValueConverter {
  public CharArrayConverter() {
    super();
  }

  @Override
  public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) throws MappingException {
    if (fromDBObject == null) {
      return null;
    }

    return fromDBObject.toString().toCharArray();
  }

  @Override
  public Object encode(final Object value, final MappedField optionalExtraInfo) {
    return new String((char[]) value);
  }
}
