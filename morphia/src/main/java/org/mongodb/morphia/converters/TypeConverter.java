package org.mongodb.morphia.converters;


import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MappingException;
import com.mongodb.DBObject;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
@SuppressWarnings("rawtypes")
public abstract class TypeConverter {
  protected Mapper mapper;
  protected Class[] supportTypes;

  protected TypeConverter() {
  }

  protected TypeConverter(final Class... types) {
    supportTypes = types;
  }

  /**
   * returns list of supported convertable types
   */
  final Class[] getSupportedTypes() {
    return supportTypes;
  }

  /**
   * checks if the class is supported for this converter.
   */
  final boolean canHandle(final Class c) {
    return isSupported(c, null);
  }

  /**
   * checks if the class is supported for this converter.
   */
  protected boolean isSupported(final Class<?> c, final MappedField optionalExtraInfo) {
    return false;
  }

  /**
   * checks if the MappedField is supported for this converter.
   */
  final boolean canHandle(final MappedField mf) {
    return isSupported(mf.getType(), mf);
  }

  /**
   * decode the {@link DBObject} and provide the corresponding java (type-safe) object<br><b>NOTE: optionalExtraInfo might be null</b>*
   */
  public abstract Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo) throws MappingException;

  /**
   * decode the {@link DBObject} and provide the corresponding java (type-safe) object *
   */
  public final Object decode(final Class targetClass, final Object fromDBObject) throws MappingException {
    return decode(targetClass, fromDBObject, null);
  }

  /**
   * encode the type safe java object into the corresponding {@link DBObject}<br><b>NOTE: optionalExtraInfo might be null</b>*
   */
  public final Object encode(final Object value) throws MappingException {
    return encode(value, null);
  }

  /**
   * checks if Class f is in classes *
   */
  protected boolean oneOf(final Class f, final Class... classes) {
    return oneOfClasses(f, classes);
  }

  /**
   * checks if Class f is in classes *
   */
  protected boolean oneOfClasses(final Class f, final Class[] classes) {
    for (final Class c : classes) {
      if (c.equals(f)) {
        return true;
      }
    }
    return false;
  }

  /**
   * encode the (type-safe) java object into the corresponding {@link DBObject}*
   */
  public Object encode(final Object value, final MappedField optionalExtraInfo) {
    return value; // as a default impl
  }

  public void setMapper(final Mapper mapper) {
    this.mapper = mapper;
  }
}
