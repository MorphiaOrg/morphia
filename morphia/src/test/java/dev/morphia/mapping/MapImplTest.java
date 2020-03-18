package dev.morphia.mapping;


import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static org.junit.Assert.assertTrue;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scott hernandez
 */
public class MapImplTest extends TestBase {

    @Test
    public void testEmbeddedMap() {
        getMapper().map(MapOfInterfaces.class);
        final Goo g1 = new Goo("Scott");
        final ContainsGoo cmoeg = new ContainsGoo();
        cmoeg.values.put("first", g1);
        getDs().save(cmoeg);
        //check className in the map values.


        MongoCollection<Document> collection = getDatabase().getCollection(getMapper()
                                                                               .getMappedClass(ContainsGoo.class).getCollectionName());
        Document first = (Document) collection
                                        .find()
                                        .first()
                                        .get("values");
        final Document goo = (Document) first.get("first");

        assertTrue(goo.toString(), goo.containsKey(getMapper().getOptions().getDiscriminatorKey()));
    }

    @Test
    public void testEmbeddedMapUpdateOperations() {
        getMapper().map(MapOfInterfaces.class);
        final Goo g1 = new Goo("Scott");
        final Goo g2 = new Goo("Ralph");

        final ContainsGoo contains = new ContainsGoo();
        contains.values.put("first", g1);
        getDs().save(contains);
        getDs().find(ContainsGoo.class)
               .filter(eq("_id", contains.id))
               .update()
               .set("values.second", g2)
               .execute();

        MongoCollection<Document> collection = getDatabase().getCollection(getMapper()
                                                                               .getMappedClass(ContainsGoo.class).getCollectionName());

        final Document goo = (Document) ((Document) collection
                                                           .find()
                                                           .first()
                                                           .get("values"))
                                            .get("second");
        assertTrue("className should not be here.", goo.containsKey(
            getMapper().getOptions().getDiscriminatorKey()));
    }

    @Test
    public void testEmbeddedMapUpdateOperationsOnInterfaceValue() {
        getMapper().map(List.of(MapOfInterfaces.class));
        final Goo g1 = new Goo("Scott");
        final Goo g2 = new Goo("Ralph");

        final MapOfInterfaces cmoei = new MapOfInterfaces();
        cmoei.values.put("first", g1);
        getDs().save(cmoei);
        getDs().find(MapOfInterfaces.class)
               .filter(eq("_id", cmoei.id))
               .update()
               .set("values.second", g2)
               .execute();

        MongoCollection<Document> collection = getDatabase()
                                                   .getCollection(getMapper()
                                                                      .getMappedClass(MapOfInterfaces.class).getCollectionName());
        //check className in the map values.
        final Document goo = (Document) ((Document) collection
                                                           .find()
                                                           .first()
                                                           .get("values"))
                                            .get("second");
        assertTrue("className should be here.", goo.containsKey(getMapper().getOptions().getDiscriminatorKey()));
    }

    @Test
    public void testEmbeddedMapWithValueInterface() {
        getMapper().map(MapOfInterfaces.class);
        final Goo g1 = new Goo("Scott");

        final MapOfInterfaces cmoei = new MapOfInterfaces();
        cmoei.values.put("first", g1);
        getDs().save(cmoei);
        //check className in the map values.
        MongoCollection<Document> collection = getDatabase()
                                                   .getCollection(getMapper()
                                                                      .getMappedClass(MapOfInterfaces.class).getCollectionName());

        final Document goo = (Document) ((Document) collection
                                                        .find()
                                                        .first()
                                                        .get("values"))
                                            .get("first");
        assertTrue(goo.containsKey(getMapper().getOptions().getDiscriminatorKey()));
    }

    @Test
    public void testMapping() {
        E e = new E();
        e.mymap.put("1", "a");
        e.mymap.put("2", "b");

        getDs().save(e);

        final Datastore datastore = getDs();
        e = datastore.find(E.class)
                     .filter(eq("_id", e.id))
                     .first();
        Assert.assertEquals("a", e.mymap.get("1"));
        Assert.assertEquals("b", e.mymap.get("2"));
    }

    @Entity
    private static class MapOfInterfaces {
        @Id
        private ObjectId id;
        private final Map<String, Goober> values = new HashMap<>();
    }

    @Entity
    private static class ContainsGoo {
        @Id
        private ObjectId id;
        private final Map<String, Goo> values = new HashMap<>();
    }

    @Embedded
    private interface Goober {}

    @Embedded
    private static class Goo implements Goober {
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
