package dev.morphia.issue173;


import dev.morphia.converters.SimpleValueConverter;
import dev.morphia.converters.TypeConverter;
import dev.morphia.mapping.MappedField;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;


public class CalendarConverter extends TypeConverter implements SimpleValueConverter {
    public CalendarConverter() {
        super(Calendar.class);
    }

    @Override
    public Object decode(final Class type, final Object o, final MappedField mf) {
        if (o == null) {
            return null;
        }
        final List values = (List) o;
        if (values.size() < 2) {
            return null;
        }
        //-- date --//
        final Date utcDate = (Date) values.get(0);
        final long millis = utcDate.getTime();

        //-- TimeZone --//
        final String timeZoneId = (String) values.get(1);
        final TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
        //-- GregorianCalendar construction --//
        final GregorianCalendar calendar = new GregorianCalendar(timeZone);
        calendar.setTimeInMillis(millis);
        return calendar;
    }

    @Override
    public Object encode(final Object val, final MappedField optionalExtraInfo) {
        if (val == null) {
            return null;
        }
        final Calendar calendar = (Calendar) val;
        final long millis = calendar.getTimeInMillis();
        // . a date so that we can see it clearly in MongoVue
        // . the date is UTC because
        //   . timeZone.getOffset(millis) - timeZone.getOffset(newMillis)  may not be 0 (if we're close to DST limits)
        //   . and it's like that inside GregorianCalendar => more natural
        final Date utcDate = new Date(millis);
        final List<Object> values = new ArrayList<Object>();
        values.add(utcDate);
        values.add(calendar.getTimeZone().getID());
        return values;
    }
}
