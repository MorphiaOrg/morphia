package org.mongodb.morphia.converters;


import org.mongodb.morphia.mapping.MappedField;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class IdentityConverter extends TypeConverter {

  public IdentityConverter(final Class... types) {
    super(types);
  }

  @Override
  protected boolean isSupported(final Class c, final MappedField optionalExtraInfo) {
    return true;
  }

  @Override
  public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) {
    return fromDBObject;
  }
}
