package org.mongodb.morphia.mapping;


import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scott hernandez
 */
public class MapImplTest extends TestBase {

    @Test
    public void testEmbeddedMap() throws Exception {
        getMorphia().map(ContainsMapOfEmbeddedGoos.class).map(ContainsMapOfEmbeddedInterfaces.class);
        final Goo g1 = new Goo("Scott");
        final ContainsMapOfEmbeddedGoos cmoeg = new ContainsMapOfEmbeddedGoos();
        cmoeg.values.put("first", g1);
        getDs().save(cmoeg);
        //check className in the map values.

        final BasicDBObject goo = (BasicDBObject) ((BasicDBObject) getDs().getCollection(ContainsMapOfEmbeddedGoos.class)
                                                                          .findOne()
                                                                          .get("values")).get(
                                                                                                 "first");
        final boolean hasF = goo.containsField(Mapper.CLASS_NAME_FIELDNAME);
        assertTrue(!hasF);
    }

    @Test //@Ignore("waiting on issue 184")
    public void testEmbeddedMapUpdateOperations() throws Exception {
        getMorphia().map(ContainsMapOfEmbeddedGoos.class).map(ContainsMapOfEmbeddedInterfaces.class);
        final Goo g1 = new Goo("Scott");
        final Goo g2 = new Goo("Ralph");

        final ContainsMapOfEmbeddedGoos cmoeg = new ContainsMapOfEmbeddedGoos();
        cmoeg.values.put("first", g1);
        getDs().save(cmoeg);
        getDs().update(cmoeg, getDs().createUpdateOperations(ContainsMapOfEmbeddedGoos.class).set("values.second", g2));
        //check className in the map values.

        final BasicDBObject goo = (BasicDBObject) ((BasicDBObject) getDs().getCollection(ContainsMapOfEmbeddedGoos.class)
                                                                          .findOne()
                                                                          .get("values")).get(
                                                                                                 "second");
        final boolean hasF = goo.containsField(Mapper.CLASS_NAME_FIELDNAME);
        assertTrue("className should not be here.", !hasF);
    }

    @Test
    public void testEmbeddedMapUpdateOperationsOnInterfaceValue() throws Exception {
        getMorphia().map(ContainsMapOfEmbeddedGoos.class).map(ContainsMapOfEmbeddedInterfaces.class);
        final Goo g1 = new Goo("Scott");
        final Goo g2 = new Goo("Ralph");

        final ContainsMapOfEmbeddedInterfaces cmoei = new ContainsMapOfEmbeddedInterfaces();
        cmoei.values.put("first", g1);
        getDs().save(cmoei);
        getDs().update(cmoei, getDs().createUpdateOperations(ContainsMapOfEmbeddedInterfaces.class).set("values.second", g2));
        //check className in the map values.
        final BasicDBObject goo = (BasicDBObject) ((BasicDBObject) getDs().getCollection(ContainsMapOfEmbeddedInterfaces.class)
                                                                          .findOne()
                                                                          .get("values"))
                                                      .get("second");
        final boolean hasF = goo.containsField(Mapper.CLASS_NAME_FIELDNAME);
        assertTrue("className should be here.", hasF);
    }

    @Test
    public void testEmbeddedMapWithValueInterface() throws Exception {
        getMorphia().map(ContainsMapOfEmbeddedGoos.class).map(ContainsMapOfEmbeddedInterfaces.class);
        final Goo g1 = new Goo("Scott");

        final ContainsMapOfEmbeddedInterfaces cmoei = new ContainsMapOfEmbeddedInterfaces();
        cmoei.values.put("first", g1);
        getDs().save(cmoei);
        //check className in the map values.
        final BasicDBObject goo = (BasicDBObject) ((BasicDBObject) getDs().getCollection(ContainsMapOfEmbeddedInterfaces.class)
                                                                          .findOne()
                                                                          .get("values"))
                                                      .get("first");
        final boolean hasF = goo.containsField(Mapper.CLASS_NAME_FIELDNAME);
        assertTrue(hasF);
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
