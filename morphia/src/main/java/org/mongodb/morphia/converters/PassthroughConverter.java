package org.mongodb.morphia.converters;


import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.MappingException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({"rawtypes" })
public class PassthroughConverter extends TypeConverter {

  public PassthroughConverter() {
  }

  public PassthroughConverter(final Class... types) {
    super(types);
  }

  @Override
  protected boolean isSupported(final Class c, final MappedField optionalExtraInfo) {
    return true;
  }

  @Override
  public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) throws MappingException {
    return fromDBObject;
  }
}
