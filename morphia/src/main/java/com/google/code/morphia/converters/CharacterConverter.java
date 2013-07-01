package com.google.code.morphia.converters;


import java.lang.reflect.Array;
import java.util.List;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({"rawtypes"})
public class CharacterConverter extends TypeConverter implements SimpleValueConverter {
  public CharacterConverter() {
    super(char.class, Character.class, char[].class, Character[].class);
  }

  @Override
  public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) throws MappingException {
    if (fromDBObject == null) {
      return null;
    }

    if (fromDBObject instanceof String) {
      final char[] chars = ((String) fromDBObject).toCharArray();
      if ((targetClass == char.class || targetClass == Character.class)) {
        if (chars.length == 1) {
          return chars[0];
        } else {
          throw new MappingException("Trying to map multicharacter data to a single character: " + fromDBObject);
        }
      }
      final Class<?> type = targetClass.isArray() ? targetClass.getComponentType() : targetClass;
      return convert(type, chars);
    }

    if (fromDBObject instanceof List) {
      final Class<?> type = targetClass.isArray() ? targetClass.getComponentType() : targetClass;
      return convert(type, ((List<String>) fromDBObject).toArray(new String[0]));
    }

    // TODO: Check length. Maybe "" should be null?
    return fromDBObject.toString().charAt(0);
  }

  private Object convert(final Class<?> type, final String[] values) {
    final Object array = Array.newInstance(type, values.length);
    for (int i = 0; i < values.length; i++) {
      Array.set(array, i, decode(type, values[i]));
    }
    return array;
  }

  public static Object convert(final Class type, final char[] values) {
    final Object array = Array.newInstance(type, values.length);
    for (int i = 0; i < values.length; i++) {
      Array.set(array, i, values[i]);
    }
    return array;
  }
}
