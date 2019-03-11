package dev.morphia.converters;

import org.junit.Test;
import dev.morphia.TestBase;

import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LocaleConverterTest extends TestBase {

    @Test
    public void shouldEncodeAndDecodeBuiltInLocale() throws Exception {
        // given
        LocaleConverter converter = new LocaleConverter();
        Locale expectedLocale = Locale.CANADA_FRENCH;

        // when
        Locale decodedLocale = (Locale) converter.decode(Locale.class, converter.encode(expectedLocale));

        // then
        assertThat(decodedLocale, is(expectedLocale));
    }

    @Test
    public void shouldEncodeAndDecodeCountryOnlyLocale() {
        // given
        LocaleConverter converter = new LocaleConverter();
        Locale expectedLocale = new Locale("", "FI");

        // when
        Locale decodedLocale = (Locale) converter.decode(Locale.class, converter.encode(expectedLocale));

        // then
        assertThat(decodedLocale, is(expectedLocale));
    }

    @Test
    public void shouldEncodeAndDecodeCustomLocale() {
        // given
        LocaleConverter converter = new LocaleConverter();
        Locale expectedLocale = new Locale("de", "DE", "bavarian");

        // when
        Locale decodedLocale = (Locale) converter.decode(Locale.class, converter.encode(expectedLocale));

        // then
        assertThat(decodedLocale, is(expectedLocale));
        assertThat(decodedLocale.getLanguage(), is("de"));
        assertThat(decodedLocale.getCountry(), is("DE"));
        assertThat(decodedLocale.getVariant(), is("bavarian"));
    }

    @Test
    public void shouldEncodeAndDecodeNoCountryLocale() {
        // given
        LocaleConverter converter = new LocaleConverter();
        Locale expectedLocale = new Locale("fi", "", "VAR");

        // when
        Locale decodedLocale = (Locale) converter.decode(Locale.class, converter.encode(expectedLocale));

        // then
        assertThat(decodedLocale, is(expectedLocale));
    }

    @Test
    public void shouldEncodeAndDecodeNoLanguageLocale() {
        // given
        LocaleConverter converter = new LocaleConverter();
        Locale expectedLocale = new Locale("", "FI", "VAR");

        // when
        Locale decodedLocale = (Locale) converter.decode(Locale.class, converter.encode(expectedLocale));

        // then
        assertThat(decodedLocale, is(expectedLocale));
    }

    @Test
    public void shouldEncodeAndDecodeSpecialVariantLocale() {
        // given
        LocaleConverter converter = new LocaleConverter();
        Locale expectedLocale = new Locale("fi", "FI", "VAR_SPECIAL");

        // when
        Locale decodedLocale = (Locale) converter.decode(Locale.class, converter.encode(expectedLocale));

        // then
        assertThat(decodedLocale, is(expectedLocale));
    }
}
