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
public class BooleanConverter extends TypeConverter implements SimpleValueConverter {

  public BooleanConverter() {
    super(boolean.class, Boolean.class, boolean[].class, Boolean[].class);
  }

  @Override
  public Object decode(final Class targetClass, final Object val, final MappedField optionalExtraInfo) throws MappingException {
    if (val == null) {
      return null;
    }

    if (val instanceof Boolean) {
      return val;
    }

    //handle the case for things like the ok field
    if (val instanceof Number) {
      return ((Number) val).intValue() == 1;
    }

    if (val instanceof List) {
      final Class<?> type = targetClass.isArray() ? targetClass.getComponentType() : targetClass;
      return ReflectionUtils.convertToArray(type, (List<?>) val);
    }

    return Boolean.parseBoolean(val.toString());
  }
}
