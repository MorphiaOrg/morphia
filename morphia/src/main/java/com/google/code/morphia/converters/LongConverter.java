package com.google.code.morphia.converters;


import java.util.List;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.utils.ReflectionUtils;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({"rawtypes" })
public class LongConverter extends TypeConverter implements SimpleValueConverter {

  public LongConverter() {
    super(long.class, Long.class, long[].class, Long[].class);
  }

  @Override
  public Object decode(final Class targetClass, final Object val, final MappedField optionalExtraInfo) throws MappingException {
    if (val == null) {
      return null;
    }

    if (val instanceof Long) {
      return val;
    }

    if (val instanceof Number) {
      return ((Number) val).longValue();
    }

    if (val instanceof List) {
      final Class<?> type = targetClass.isArray() ? targetClass.getComponentType() : targetClass;
      return ReflectionUtils.convertToArray(type, (List<?>) val);
    }

    return Long.parseLong(val.toString());
  }

}
