package org.mongodb.morphia.converters;


import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.MappingException;
import com.mongodb.DBRef;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({"rawtypes" })
public class KeyConverter extends TypeConverter {

  public KeyConverter() {
    super(Key.class);
  }

  @Override
  public Object decode(final Class targetClass, final Object o, final MappedField optionalExtraInfo) throws MappingException {
    if (o == null) {
      return null;
    }
    if (!(o instanceof DBRef)) {
      throw new ConverterException(String.format("cannot convert %s to Key because it isn't a DBRef", o.toString()));
    }

    return mapper.refToKey((DBRef) o);
  }

  @Override
  public Object encode(final Object t, final MappedField optionalExtraInfo) {
    if (t == null) {
      return null;
    }
    return mapper.keyToRef((Key) t);
  }

}
