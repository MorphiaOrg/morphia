package dev.morphia.mapping;


import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;


/**
 * @author scott hernandez
 */
public class MapWithDotInKeyTest extends TestBase {

    @Test
    public void testMapping() {
        E e = new E();
        e.mymap.put("a.b", "a");
        e.mymap.put("c.e.g", "b");

        try {
            getDs().save(e);
        } catch (Exception ex) {
            return;
        }

        Assert.assertFalse("Should have got rejection for dot in field names", true);
        e = getDs().get(e);
        Assert.assertEquals("a", e.mymap.get("a.b"));
        Assert.assertEquals("b", e.mymap.get("c.e.g"));
    }

    @Entity
    private static class Goo implements Serializable {
        @Id
        private ObjectId id = new ObjectId();
        private String name;

        Goo() {
        }

        Goo(final String n) {
            name = n;
        }
    }

    @Entity
    private static class E {
        @Id
        private ObjectId id;
        private final MyMap mymap = new MyMap();
    }

    @Embedded
    private static class MyMap extends Document {
    }
}
