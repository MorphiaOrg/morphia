package com.google.code.morphia.converters;


import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({"rawtypes" })
public class ClassConverter extends TypeConverter implements SimpleValueConverter {

  public ClassConverter() {
    super(Class.class);
  }

  @Override
  public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) throws MappingException {
    if (fromDBObject == null) {
      return null;
    }

    final String l = fromDBObject.toString();
    try {
      return Class.forName(l);
    } catch (ClassNotFoundException e) {
      throw new MappingException("Cannot create class from Name '" + l + "'", e);
    }
  }

  @Override
  public Object encode(final Object value, final MappedField optionalExtraInfo) {
    if (value == null) {
      return null;
    } else {
      return ((Class) value).getName();
    }
  }
}
