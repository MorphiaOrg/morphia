package com.google.code.morphia.converters;


import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({"rawtypes" })
public class CharacterConverter extends TypeConverter implements SimpleValueConverter {
  public CharacterConverter() {
    super(Character.class, char.class);
  }

  @Override
  public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) throws MappingException {
    if (fromDBObject == null) {
      return null;
    }

    // TODO: Check length. Maybe "" should be null?
    return fromDBObject.toString().charAt(0);
  }

  @Override
  public Object encode(final Object value, final MappedField optionalExtraInfo) {
    return String.valueOf(value);
  }
}
