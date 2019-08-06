package dev.morphia.mapping;


import dev.morphia.annotations.Entity;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Id;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scott hernandez
 */
public class MapImplTest extends TestBase {

    @Test
    public void testEmbeddedMap() {
        getMapper().map(ContainsMapOfEmbeddedInterfaces.class);
        final Goo g1 = new Goo("Scott");
        final ContainsMapOfEmbeddedGoos cmoeg = new ContainsMapOfEmbeddedGoos();
        cmoeg.values.put("first", g1);
        getDs().save(cmoeg);
        //check className in the map values.

        Document first = (Document) getDatabase().getCollection(ContainsMapOfEmbeddedInterfaces.class.getSimpleName())
                                                 .find()
                                                 .first()
                                                 .get("values");
        final Document goo = (Document) first.get("first");

        assertFalse(goo.containsKey(getMapper().getOptions().getDiscriminatorField()));
    }

    @Test
    public void testEmbeddedMapUpdateOperations() {
        getMapper().map(ContainsMapOfEmbeddedInterfaces.class);
        final Goo g1 = new Goo("Scott");
        final Goo g2 = new Goo("Ralph");

        final ContainsMapOfEmbeddedGoos cmoeg = new ContainsMapOfEmbeddedGoos();
        cmoeg.values.put("first", g1);
        getDs().save(cmoeg);
        getDs().find(ContainsMapOfEmbeddedGoos.class)
               .filter("_id", cmoeg.id)
               .update()
               .set("values.second", g2)
               .execute();

        final Document goo = (Document) ((Document) getDatabase().getCollection(ContainsMapOfEmbeddedGoos.class.getSimpleName())
                                                           .find()
                                                           .first()
                                                           .get("values")).get(
            "second");
        assertFalse("className should not be here.", goo.containsKey(
            getMapper().getOptions().getDiscriminatorField()));
    }

    @Test
    public void testEmbeddedMapUpdateOperationsOnInterfaceValue() {
        getMapper().map(ContainsMapOfEmbeddedInterfaces.class);
        final Goo g1 = new Goo("Scott");
        final Goo g2 = new Goo("Ralph");

        final ContainsMapOfEmbeddedInterfaces cmoei = new ContainsMapOfEmbeddedInterfaces();
        cmoei.values.put("first", g1);
        getDs().save(cmoei);
        getDs().find(ContainsMapOfEmbeddedInterfaces.class)
               .filter("_id", cmoei.id)
               .update()
               .set("values.second", g2)
               .execute();

        //check className in the map values.
        final Document goo = (Document) ((Document) getDatabase().getCollection(ContainsMapOfEmbeddedInterfaces.class.getSimpleName())
                                                           .find()
                                                           .first()
                                                           .get("values"))
                                            .get("second");
        assertTrue("className should be here.", goo.containsKey(getMapper().getOptions().getDiscriminatorField()));
    }

    @Test
    public void testEmbeddedMapWithValueInterface() {
        getMapper().map(ContainsMapOfEmbeddedInterfaces.class);
        final Goo g1 = new Goo("Scott");

        final ContainsMapOfEmbeddedInterfaces cmoei = new ContainsMapOfEmbeddedInterfaces();
        cmoei.values.put("first", g1);
        getDs().save(cmoei);
        //check className in the map values.
        final Document goo = (Document) ((Document) getDatabase().getCollection(ContainsMapOfEmbeddedInterfaces.class.getSimpleName())
                                                           .find()
                                                           .first()
                                                           .get("values"))
                                            .get("first");
        assertTrue(goo.containsKey(getMapper().getOptions().getDiscriminatorField()));
    }

    @Test
    public void testMapping() {
        E e = new E();
        e.mymap.put("1", "a");
        e.mymap.put("2", "b");

        getDs().save(e);

        e = getDs().get(e);
        Assert.assertEquals("a", e.mymap.get("1"));
        Assert.assertEquals("b", e.mymap.get("2"));
    }

    @Entity
    private static class ContainsMapOfEmbeddedInterfaces {
        @Id
        private ObjectId id;
        private final Map<String, Serializable> values = new HashMap<>();
    }

    @Entity
    private static class ContainsMapOfEmbeddedGoos {
        @Id
        private ObjectId id;
        private final Map<String, Goo> values = new HashMap<>();
    }

    @Embedded
    private static class Goo implements Serializable {
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
    private static class MyMap extends HashMap<String, String> {
    }
}
