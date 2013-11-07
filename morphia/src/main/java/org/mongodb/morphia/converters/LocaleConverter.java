package org.mongodb.morphia.converters;


import org.mongodb.morphia.mapping.MappedField;

import java.util.Locale;
import java.util.StringTokenizer;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class LocaleConverter extends TypeConverter implements SimpleValueConverter {

  public LocaleConverter() {
    super(Locale.class);
  }

  @Override
  public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) {
    return parseLocale(fromDBObject.toString());
  }

  @Override
  public Object encode(final Object val, final MappedField optionalExtraInfo) {
    if (val == null) {
      return null;
    }

    return val.toString();
  }

  public static Locale parseLocale(final String localeString) {
    if ((localeString != null) && (localeString.length() != 0)) {
      final StringTokenizer st = new StringTokenizer(localeString, "_");
      final String language = st.hasMoreElements() ? st.nextToken() : Locale.getDefault().getLanguage();
      final String country = st.hasMoreElements() ? st.nextToken() : "";
      final String variant = st.hasMoreElements() ? st.nextToken() : "";
      return new Locale(language, country, variant);
    }
    return null;
  }
}
