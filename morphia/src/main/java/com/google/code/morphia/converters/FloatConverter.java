package com.google.code.morphia.converters;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.bson.LazyBSONList;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({"rawtypes"})
public class FloatConverter extends TypeConverter implements SimpleValueConverter {

  public FloatConverter() {
    super(float.class, Float.class, float[].class, Float[].class);
  }

  @Override
  public Object decode(final Class targetClass, final Object val, final MappedField optionalExtraInfo) throws MappingException {
    if (val == null) {
      return null;
    }

    if (val instanceof Float) {
      return val;
    }

    if (val instanceof Number) {
      return ((Number) val).floatValue();
    }

    //FixMe: super-hacky
    if (val instanceof LazyBSONList || val instanceof ArrayList) {
      final Class<?> type = targetClass.isArray() ? targetClass.getComponentType() : targetClass;
      return convertToArray(type, (List<?>) val);
    }

    return Float.parseFloat(val.toString());
  }

  private Object convertToArray(final Class type, final List<?> values) {
      final Object exampleArray = Array.newInstance(type, values.size());
      try {
        return values.toArray((Object[]) exampleArray);
      } catch (Exception e) {
        for (int i = 0; i < values.size(); i++) {
          Array.set(exampleArray, i, decode(Float.class, values.get(i)));
        }
        return exampleArray;
      }
    }
}
