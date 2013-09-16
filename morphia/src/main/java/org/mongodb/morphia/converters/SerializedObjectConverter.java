package org.mongodb.morphia.converters;


import java.io.IOException;

import org.bson.types.Binary;
import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.MappingException;
import org.mongodb.morphia.mapping.Serializer;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class SerializedObjectConverter extends TypeConverter {
  @Override
  protected boolean isSupported(final Class c, final MappedField optionalExtraInfo) {
    return optionalExtraInfo != null && (optionalExtraInfo.hasAnnotation(Serialized.class));
  }

  @Override
  public Object decode(final Class targetClass, final Object fromDBObject, final MappedField f) throws MappingException {
    if (fromDBObject == null) {
      return null;
    }

    if (!((fromDBObject instanceof Binary) || (fromDBObject instanceof byte[]))) {
      throw new MappingException(
        "The stored data is not a DBBinary or byte[] instance for " + f.getFullName() + " ; it is a " + fromDBObject.getClass().getName());
    }

    try {
      final boolean useCompression = !f.getAnnotation(Serialized.class).disableCompression();
      return Serializer.deserialize(fromDBObject, useCompression);
    } catch (IOException e) {
      throw new MappingException("While deserializing to " + f.getFullName(), e);
    } catch (ClassNotFoundException e) {
      throw new MappingException("While deserializing to " + f.getFullName(), e);
    }
  }

  @Override
  public Object encode(final Object value, final MappedField f) {
    if (value == null) {
      return null;
    }
    try {
      final boolean useCompression = !f.getAnnotation(Serialized.class).disableCompression();
      return Serializer.serialize(value, useCompression);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
