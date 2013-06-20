package com.google.code.morphia.converters;


import java.util.UUID;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;


/**
 * provided by http://code.google.com/p/morphia/issues/detail?id=320
 *
 * @author stummb
 * @author scotthernandez
 */
@SuppressWarnings({ "rawtypes" })
public class UUIDConverter extends TypeConverter implements SimpleValueConverter {

  public UUIDConverter() {
    super(UUID.class);
  }

  @Override
  public Object encode(final Object value, final MappedField optionalExtraInfo) {
    return value == null ? null : value.toString();
  }

  @Override
  public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) throws MappingException {
    return fromDBObject == null ? null : UUID.fromString((String) fromDBObject);
  }
}