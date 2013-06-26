package com.google.code.morphia.converters;


import java.util.ArrayList;
import java.util.List;

import org.bson.LazyBSONList;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.utils.ReflectionUtils;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({ "rawtypes" })
public class DoubleConverter extends TypeConverter implements SimpleValueConverter {

  public DoubleConverter() {
    super(double.class, Double.class);
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

    //FixMe: super-hacky
    if (val instanceof LazyBSONList || val instanceof ArrayList) {
      return ReflectionUtils.convertToArray(Double.class, (List<?>) val);
    }

    final String sVal = val.toString();
    return Double.parseDouble(sVal);
  }
}
