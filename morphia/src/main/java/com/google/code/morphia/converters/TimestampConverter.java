package com.google.code.morphia.converters;


import java.sql.Timestamp;
import java.util.Date;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;


/**
 * @author scotthernandez
 */
@SuppressWarnings({ "rawtypes" })
public class TimestampConverter extends DateConverter {

  public TimestampConverter() {
    super(Timestamp.class);
  }

  @Override
  public Object decode(final Class targetClass, final Object val, final MappedField optionalExtraInfo) throws MappingException {
    final Date d = (Date) super.decode(targetClass, val, optionalExtraInfo);
    return new Timestamp(d.getTime());
  }

  @Override
  public Object encode(final Object val, final MappedField optionalExtraInfo) {
    if (val == null) {
      return null;
    }
    return new Date(((Timestamp) val).getTime());
  }

}
