package org.mongodb.morphia.converters;


import java.util.Locale;

import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.junit.Assert;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class LocaleConverterTest extends TestBase {
  @Test
  public void testConversion() throws Exception {
    final LocaleConverter c = new LocaleConverter();

    Locale l = Locale.CANADA_FRENCH;
    Locale l2 = (Locale) c.decode(Locale.class, c.encode(l));
    Assert.assertEquals(l, l2);

    l = new Locale("de", "DE", "bavarian");
    l2 = (Locale) c.decode(Locale.class, c.encode(l));
    Assert.assertEquals(l, l2);
    Assert.assertEquals("de", l2.getLanguage());
    Assert.assertEquals("DE", l2.getCountry());
    Assert.assertEquals("bavarian", l2.getVariant());

    // Incomplete locales
    l = new Locale("", "FI");
    l2 = (Locale) c.decode(Locale.class, c.encode(l));
    Assert.assertEquals(l, l2);
    
    l = new Locale("fi", "", "VAR");
    l2 = (Locale) c.decode(Locale.class, c.encode(l));
    Assert.assertEquals(l, l2);
    
    l = new Locale("", "FI", "VAR");
    l2 = (Locale) c.decode(Locale.class, c.encode(l));
    Assert.assertEquals(l, l2);
    
    l = new Locale("fi", "FI", "VAR_STRANGE");
    l2 = (Locale) c.decode(Locale.class, c.encode(l));
    Assert.assertEquals(l, l2);
  }
}
