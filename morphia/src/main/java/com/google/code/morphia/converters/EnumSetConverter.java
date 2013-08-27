package com.google.code.morphia.converters;


import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class EnumSetConverter extends TypeConverter implements SimpleValueConverter {

  private final EnumConverter ec = new EnumConverter();

  public EnumSetConverter() {
    super(EnumSet.class);
  }

  @Override
  public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) throws MappingException {
    if (fromDBObject == null) {
      return null;
    }

    final Class enumType = optionalExtraInfo.getSubClass();

    final List l = (List) fromDBObject;
    if (l.isEmpty()) {
      return EnumSet.noneOf(enumType);
    }

    final List enums = new ArrayList();
    for (final Object object : l) {
      enums.add(ec.decode(enumType, object));
    }
    return EnumSet.copyOf(enums);
  }

  @Override
  public Object encode(final Object value, final MappedField optionalExtraInfo) {
    if (value == null) {
      return null;
    }

    final List values = new ArrayList();

    final EnumSet s = (EnumSet) value;
    final Object[] array = s.toArray();
    for (final Object anArray : array) {
      values.add(ec.encode(anArray));
    }

    return values;
  }
}
