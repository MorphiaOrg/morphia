package org.mongodb.morphia.mapping;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.lazy.ProxyTestBase;
import org.mongodb.morphia.query.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mongodb.morphia.mapping.lazy.LazyFeatureDependencies.testDependencyFullFilled;

/**
 * @author Gene Trog, (eternal0@github.com)
 */
public class ReferenceTest extends ProxyTestBase {
    @Test
    public void testFindByEntityReference() {
        final Ref ref = new Ref("refId");
        getDs().save(ref);

        final Container container = new Container();
        container.singleRef = ref;
        getDs().save(container);

        Assert.assertNotNull(getDs().find(Container.class, "singleRef", ref).get());
    }

    @Test
    public void testIdOnlyReferences() {
        final Ref[] refs = new Ref[]{new Ref("foo"), new Ref("bar"), new Ref("baz")};
        final Container c = new Container(refs);

        // test that we can save it
        final Key<Container> key = getDs().save(c);
        getDs().save(refs);

        // ensure that we're not using DBRef
        final DBCollection collection = getDs().getCollection(Container.class);
        final DBObject persisted = collection.findOne(key.getId());
        assertNotNull(persisted);
        assertEquals("foo", persisted.get("singleRef"));
        assertEquals("foo", persisted.get("lazySingleRef"));

        final BasicDBList expectedList = new BasicDBList();
        expectedList.add("foo");
        expectedList.add("bar");
        expectedList.add("baz");
        assertEquals(expectedList, persisted.get("collectionRef"));
        assertEquals(expectedList, persisted.get("lazyCollectionRef"));

        final DBObject expectedMap = new BasicDBObject();
        expectedMap.put("0", "foo");
        expectedMap.put("1", "bar");
        expectedMap.put("2", "baz");
        assertEquals(expectedMap, persisted.get("mapRef"));
        assertEquals(expectedMap, persisted.get("lazyMapRef"));

        // ensure that we can retrieve it
        final Container retrieved = getDs().getByKey(Container.class, key);

        assertEquals(refs[0], retrieved.getSingleRef());
        if (testDependencyFullFilled()) {
            assertIsProxy(retrieved.getLazySingleRef());
        }
        assertEquals(refs[0], unwrap(retrieved.getLazySingleRef()));

        final List<Ref> expectedRefList = new ArrayList<Ref>();
        final Map<Integer, Ref> expectedRefMap = new LinkedHashMap<Integer, Ref>();

        for (int i = 0; i < refs.length; i++) {
            expectedRefList.add(refs[i]);
            expectedRefMap.put(i, refs[i]);
        }

        assertEquals(expectedRefList, retrieved.getCollectionRef());
        assertEquals(expectedRefList, unwrapList(retrieved.getLazyCollectionRef()));
        assertEquals(expectedRefMap, retrieved.getMapRef());
        assertEquals(expectedRefMap, unwrapMap(retrieved.getLazyMapRef()));
    }

    @Test
    public void testNullReferences() {
        Container container = new Container();
        container.lazyMapRef = null;
        container.singleRef = null;
        container.lazySingleRef = null;
        container.collectionRef = null;
        container.lazyCollectionRef = null;
        container.mapRef = null;
        container.lazyMapRef = null;

        getMorphia().getMapper().getOptions().setStoreNulls(true);
        getDs().save(container);
        allNull(container);

        getMorphia().getMapper().getOptions().setStoreNulls(false);
        getDs().save(container);
        allNull(container);
    }

    @Test
    public void testReferenceQueryWithoutValidation() {
        Ref ref = new Ref("no validation");
        getDs().save(ref);
        final Container container = new Container(ref);
        getDs().save(container);
        final Query<Container> query = getDs().createQuery(Container.class)
//                                             .disableValidation()
                                             .field("singleRef").equal(ref);
        Assert.assertNotNull(query.get());
    }

    @Test
    public void testReferencesWithoutMapping() throws Exception {
        Child child1 = new Child();
        getDs().save(child1);

        Parent parent1 = new Parent();
        parent1.children.add(child1);
        getDs().save(parent1);

        List<Parent> parentList = getDs().find(Parent.class).asList();
        Assert.assertEquals(1, parentList.size());

        // reset Datastore to reset internal Mapper cache, so Child class
        // already cached by previous save is cleared
        Datastore localDs = getMorphia().createDatastore(getMongoClient(), new Mapper(), getDb().getName());

        parentList = localDs.find(Parent.class).asList();
        Assert.assertEquals(1, parentList.size());
    }

    private void allNull(final Container container) {
        Assert.assertNull(container.lazyMapRef);
        Assert.assertNull(container.singleRef);
        Assert.assertNull(container.lazySingleRef);
        Assert.assertNull(container.collectionRef);
        Assert.assertNull(container.lazyCollectionRef);
        Assert.assertNull(container.mapRef);
    }

    public static class Container {
        @Id
        private ObjectId id;

        @Reference(idOnly = true)
        private Ref singleRef;

        @Reference(idOnly = true, lazy = true)
        private Ref lazySingleRef;

        @Reference(idOnly = true)
        private List<Ref> collectionRef;

        @Reference(idOnly = true, lazy = true)
        private List<Ref> lazyCollectionRef;

        @Reference(idOnly = true)
        private LinkedHashMap<Integer, Ref> mapRef;

        @Reference(idOnly = true, lazy = true)
        private LinkedHashMap<Integer, Ref> lazyMapRef;

        /* required by morphia */
        public Container() {
        }

        public Container(final Ref... refs) {
            singleRef = refs[0];
            lazySingleRef = refs[0];
            collectionRef = Arrays.asList(refs);
            lazyCollectionRef = Arrays.asList(refs);
            mapRef = new LinkedHashMap<Integer, Ref>();
            lazyMapRef = new LinkedHashMap<Integer, Ref>();

            for (int i = 0; i < refs.length; i++) {
                mapRef.put(i, refs[i]);
                lazyMapRef.put(i, refs[i]);
            }
        }

        ObjectId getId() {
            return id;
        }

        Ref getSingleRef() {
            return singleRef;
        }

        Ref getLazySingleRef() {
            return lazySingleRef;
        }

        List<Ref> getCollectionRef() {
            return collectionRef;
        }

        List<Ref> getLazyCollectionRef() {
            return lazyCollectionRef;
        }

        LinkedHashMap<Integer, Ref> getMapRef() {
            return mapRef;
        }

        LinkedHashMap<Integer, Ref> getLazyMapRef() {
            return lazyMapRef;
        }
    }

    @Entity
    public static class Ref {
        @Id
        private String id;

        public Ref() {
        }

        public Ref(final String id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Ref)) {
                return false;
            }

            final Ref ref = (Ref) o;

            if (id != null ? !id.equals(ref.id) : ref.id != null) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return String.format("Ref{id='%s'}", id);
        }
    }

    @Entity(value = "children", noClassnameStored = true)
    public static class Child {
        @Id
        private ObjectId id;
    }

    @Entity(value = "parents", noClassnameStored = true)
    public static class Parent {
        @Id
        private ObjectId id;

        @Reference(lazy = true)
        private List<Child> children = new ArrayList<Child>();
    }
}
