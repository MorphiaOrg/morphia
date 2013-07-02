package com.google.code.morphia.converters;


import java.util.List;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.utils.ReflectionUtils;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({"rawtypes"})
public class StringConverter extends TypeConverter implements SimpleValueConverter {
  public StringConverter() {
    super(String.class, String[].class);
  }

  @Override
  public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) throws MappingException {
    if (fromDBObject == null) {
      return null;
    }

    if (targetClass.equals(fromDBObject.getClass())) {
      return fromDBObject;
    }

    if (fromDBObject instanceof List) {
      final Class<?> type = targetClass.isArray() ? targetClass.getComponentType() : targetClass;
      return ReflectionUtils.convertToArray(type, (List<?>) fromDBObject);
    }

    if (targetClass.equals(String[].class)) {
      return new String[] { fromDBObject.toString() };
    }

    return fromDBObject.toString();
  }
}
