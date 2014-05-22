package org.mongodb.morphia.issue173;

import com.mongodb.WriteConcern;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.testutil.TestEntity;

import java.util.Calendar;

public class TestCalendar extends TestBase {

    @Test
    public final void testCalendar() {
        getMorphia().map(A.class);
        final A a = new A();
        a.c = Calendar.getInstance();
        getDs().save(a, WriteConcern.SAFE);
        // occasionally failed, so i suspected a race cond.
        final A loaded = getDs().find(A.class).get();
        Assert.assertNotNull(loaded.c);
        Assert.assertEquals(a.c, loaded.c);
    }

    @Converters(CalendarConverter.class)
    private static class A extends TestEntity {
        private Calendar c;
    }

}
