package dev.morphia.issue173;

import com.mongodb.WriteConcern;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.InsertOptions;
import dev.morphia.TestBase;
import dev.morphia.annotations.Converters;
import dev.morphia.query.FindOptions;
import dev.morphia.testutil.TestEntity;

import java.util.Calendar;

public class TestCalendar extends TestBase {

    @Test
    public final void testCalendar() {
        getMorphia().map(A.class);
        final A a = new A();
        a.c = Calendar.getInstance();
        getDs().save(a, new InsertOptions()
            .writeConcern(WriteConcern.ACKNOWLEDGED));
        // occasionally failed, so i suspected a race cond.
        final A loaded = getDs().find(A.class)
                                .find(new FindOptions().limit(1))
                                .tryNext();
        Assert.assertNotNull(loaded.c);
        Assert.assertEquals(a.c, loaded.c);
    }

    @Converters(CalendarConverter.class)
    private static class A extends TestEntity {
        private Calendar c;
    }

}
