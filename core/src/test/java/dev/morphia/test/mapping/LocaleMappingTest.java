package dev.morphia.test.mapping;

import dev.morphia.Datastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static org.testng.Assert.assertEquals;

public class LocaleMappingTest extends TestBase {

    @Test
    public void testLocaleMapping() {
        E e = new E();
        e.l1 = Locale.CANADA_FRENCH;
        e.l2 = Arrays.asList(Locale.GERMANY, Locale.TRADITIONAL_CHINESE);
        e.l3 = new Locale[]{Locale.TRADITIONAL_CHINESE, Locale.FRENCH};

        getDs().save(e);
        final Datastore datastore = getDs();
        e = datastore.find(E.class)
                     .filter(eq("_id", e.id))
                     .first();

        assertEquals(e.l1, Locale.CANADA_FRENCH);

        assertEquals(e.l2.size(), 2);
        assertEquals(e.l2.get(0), Locale.GERMANY);
        assertEquals(e.l2.get(1), Locale.TRADITIONAL_CHINESE);

        assertEquals(e.l3.length, 2);
        assertEquals(e.l3[0], Locale.TRADITIONAL_CHINESE);
        assertEquals(e.l3[1], Locale.FRENCH);

    }

    @Entity
    public static class E {
        @Id
        private ObjectId id;
        private Locale l1;

        private List<Locale> l2 = new ArrayList<>();

        private Locale[] l3;
    }
}
