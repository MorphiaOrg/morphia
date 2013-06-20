package com.google.code.morphia.converters;


import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({"rawtypes" })
public class StringConverter extends TypeConverter implements SimpleValueConverter {
  public StringConverter() {
    super(String.class);
  }

  @Override
  public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) throws MappingException {
    if (fromDBObject == null) {
      return null;
    }

    if (fromDBObject instanceof String) {
      return fromDBObject;
    }

    return fromDBObject.toString();
  }
}
