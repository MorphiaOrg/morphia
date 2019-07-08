package dev.morphia.mapping;


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
    public void testEmbeddedMap() throws Exception {
        Mapper.map(ContainsMapOfEmbeddedInterfaces.class);
        final Goo g1 = new Goo("Scott");
        final ContainsMapOfEmbeddedGoos cmoeg = new ContainsMapOfEmbeddedGoos();
        cmoeg.values.put("first", g1);
        getDs().save(cmoeg);
        //check className in the map values.

        final Document goo = (Document) ((Document) getDs().getCollection(ContainsMapOfEmbeddedGoos.class)
                                                           .find()
                                                           .first()
                                                           .get("values"))
                                            .get("first");
        assertFalse(goo.containsKey(getMorphia().getMapper().getOptions().getDiscriminatorField()));
    }

    @Test
    public void testEmbeddedMapUpdateOperations() {
        Mapper.map(ContainsMapOfEmbeddedInterfaces.class);
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

        final Document goo = (Document) ((Document) getDs().getCollection(ContainsMapOfEmbeddedGoos.class)
                                                           .find()
                                                           .first()
                                                           .get("values")).get(
            "second");
        assertFalse("className should not be here.", goo.containsKey(
            getMorphia().getMapper().getOptions().getDiscriminatorField()));
    }

    @Test
    public void testEmbeddedMapUpdateOperationsOnInterfaceValue() throws Exception {
        Mapper.map(ContainsMapOfEmbeddedInterfaces.class);
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
        final Document goo = (Document) ((Document) getDs().getCollection(ContainsMapOfEmbeddedInterfaces.class)
                                                           .find()
                                                           .first()
                                                           .get("values"))
                                            .get("second");
        assertTrue("className should be here.", goo.containsKey(getMorphia().getMapper().getOptions().getDiscriminatorField()));
    }

    @Test
    public void testEmbeddedMapWithValueInterface() throws Exception {
        Mapper.map(ContainsMapOfEmbeddedInterfaces.class);
        final Goo g1 = new Goo("Scott");

        final ContainsMapOfEmbeddedInterfaces cmoei = new ContainsMapOfEmbeddedInterfaces();
        cmoei.values.put("first", g1);
        getDs().save(cmoei);
        //check className in the map values.
        final Document goo = (Document) ((Document) getDs().getCollection(ContainsMapOfEmbeddedInterfaces.class)
                                                           .find()
                                                           .first()
                                                           .get("values"))
                                            .get("first");
        assertTrue(goo.containsKey(getMorphia().getMapper().getOptions().getDiscriminatorField()));
    }

    @Test
    public void testMapping() throws Exception {
        E e = new E();
        e.mymap.put("1", "a");
        e.mymap.put("2", "b");

        getDs().save(e);

        e = getDs().get(e);
        Assert.assertEquals("a", e.mymap.get("1"));
        Assert.assertEquals("b", e.mymap.get("2"));
    }

    private static class ContainsMapOfEmbeddedInterfaces {
        @Embedded
        private final Map<String, Serializable> values = new HashMap<String, Serializable>();
        @Id
        private ObjectId id;
    }

    private static class ContainsMapOfEmbeddedGoos {
        private final Map<String, Goo> values = new HashMap<String, Goo>();
        @Id
        private ObjectId id;
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

    private static class E {
        @Embedded
        private final MyMap mymap = new MyMap();
        @Id
        private ObjectId id;
    }

    private static class MyMap extends HashMap<String, String> {
    }
}
