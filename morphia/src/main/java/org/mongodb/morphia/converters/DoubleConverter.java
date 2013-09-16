package org.mongodb.morphia.converters;


import java.util.List;

import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.MappingException;
import org.mongodb.morphia.utils.ReflectionUtils;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({ "rawtypes" })
public class DoubleConverter extends TypeConverter implements SimpleValueConverter {

  public DoubleConverter() {
    super(double.class, Double.class, double[].class, Double[].class);
  }

  @Override
  public Object decode(final Class targetClass, final Object val, final MappedField optionalExtraInfo) throws MappingException {
    if (val == null) {
      return null;
    }

    if (val instanceof Double) {
      return val;
    }

    if (val instanceof Number) {
      return ((Number) val).doubleValue();
    }

    if (val instanceof List) {
      final Class<?> type = targetClass.isArray() ? targetClass.getComponentType() : targetClass;
      return ReflectionUtils.convertToArray(type, (List<?>) val);
    }

    return Double.parseDouble(val.toString());
  }
}
