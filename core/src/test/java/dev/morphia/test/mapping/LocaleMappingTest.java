package dev.morphia.test.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import dev.morphia.Datastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;

public class LocaleMappingTest extends TestBase {

    @Test
    public void testLocaleMapping() {
        E e = new E();
        e.l1 = Locale.CANADA_FRENCH;
        e.l2 = Arrays.asList(Locale.GERMANY, Locale.TRADITIONAL_CHINESE);
        e.l3 = new Locale[] { Locale.TRADITIONAL_CHINESE, Locale.FRENCH };

        getDs().save(e);
        final Datastore datastore = getDs();
        e = datastore.find(E.class)
                .filter(eq("_id", e.id))
                .first();

        Assertions.assertEquals(Locale.CANADA_FRENCH, e.l1);

        Assertions.assertEquals(2, e.l2.size());
        Assertions.assertEquals(Locale.GERMANY, e.l2.get(0));
        Assertions.assertEquals(Locale.TRADITIONAL_CHINESE, e.l2.get(1));

        Assertions.assertEquals(2, e.l3.length);
        Assertions.assertEquals(Locale.TRADITIONAL_CHINESE, e.l3[0]);
        Assertions.assertEquals(Locale.FRENCH, e.l3[1]);

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
