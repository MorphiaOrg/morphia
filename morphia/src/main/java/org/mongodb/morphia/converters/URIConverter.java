package org.mongodb.morphia.converters;


import java.net.URI;

import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.MappingException;


/**
 * @author scotthernandez
 */
@SuppressWarnings({ "rawtypes" })
public class URIConverter extends TypeConverter implements SimpleValueConverter {

  public URIConverter() {
    this(URI.class);
  }

  protected URIConverter(final Class clazz) {
    super(clazz);
  }

  @Override
  public String encode(final Object uri, final MappedField optionalExtraInfo) {
    if (uri == null) {
      return null;
    }

    return uri.toString().replace(".", "%46");
  }

  @Override
  public Object decode(final Class targetClass, final Object val, final MappedField optionalExtraInfo) throws MappingException {
    if (val == null) {
      return null;
    }

    return URI.create(val.toString().replace("%46", "."));
  }
}
