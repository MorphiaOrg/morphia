package org.mongodb.morphia.mapping;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.lazy.ProxyTestBase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mongodb.morphia.mapping.lazy.LazyFeatureDependencies.testDependencyFullFilled;

/**
 * @author Gene Trog, (eternal0@github.com)
 */
public class ReferencesNotUsingDBRefTest extends ProxyTestBase {
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

        public Container(final Ref ... refs) {
            singleRef = refs[0];
            lazySingleRef = refs[0];
            collectionRef = new ArrayList<Ref>();
            lazyCollectionRef = new ArrayList<Ref>();
            mapRef = new LinkedHashMap<Integer, Ref>();
            lazyMapRef = new LinkedHashMap<Integer, Ref>();

            for (int i = 0; i < refs.length; i++) {
                collectionRef.add(refs[i]);
                lazyCollectionRef.add(refs[i]);
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

        public Ref(final String id) {
            this.id = id;
        }

        public Ref() {
        }

        public String getId() {
            return id;
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
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "Ref{"
                    + "id='"
                    + id + '\''
                    + '}';
        }
    }

    @Test
    public void testSave() {
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
    public void testFindByEntityReference() {
        final Ref ref = new Ref("refId");
        getDs().save(ref);

        final Container container = new Container();
        container.singleRef = ref;
        getDs().save(container);
        
        Assert.assertNotNull(getDs().find(Container.class, "singleRef", ref).get());
    }

    private void allNull(final Container container) {
        Assert.assertNull(container.lazyMapRef);
        Assert.assertNull(container.singleRef);
        Assert.assertNull(container.lazySingleRef);
        Assert.assertNull(container.collectionRef);
        Assert.assertNull(container.lazyCollectionRef);
        Assert.assertNull(container.mapRef);
        Assert.assertNull(container.lazyMapRef);
        
    }
}
