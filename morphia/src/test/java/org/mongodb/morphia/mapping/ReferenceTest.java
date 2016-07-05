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
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.lazy.ProxyTestBase;
import org.mongodb.morphia.query.MorphiaKeyIterator;
import org.mongodb.morphia.query.Query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mongodb.morphia.mapping.lazy.LazyFeatureDependencies.testDependencyFullFilled;

/**
 * @author Gene Trog, (eternal0@github.com)
 */
public class ReferenceTest extends ProxyTestBase {
    @Test
    public void testComplexIds() {
        Complex complex = new Complex(new ChildId("Bob", 67), "Kelso");
        List<Complex> list = asList(new Complex(new ChildId("Turk", 27), "Turk"),
                                    new Complex(new ChildId("JD", 26), "Dorian"),
                                    new Complex(new ChildId("Carla", 29), "Espinosa"));
        List<Complex> lazyList = asList(new Complex(new ChildId("Bippity", 67), "Boppity"),
                                    new Complex(new ChildId("Cinder", 22), "Ella"),
                                    new Complex(new ChildId("Prince", 29), "Charming"));

        ComplexParent parent = new ComplexParent();
        parent.complex = complex;
        parent.list = list;
        parent.lazyList = lazyList;

        getDs().save(complex);
        getDs().save(list);
        getDs().save(lazyList);
        getDs().save(parent);

        ComplexParent complexParent = getDs().get(ComplexParent.class, parent.id);
        assertEquals(parent, complexParent);
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
    public void testReferencesWithoutMapping() {
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

    @Test
    public void testFetchKeys() {
        List<Complex> list = asList(new Complex(new ChildId("Turk", 27), "Turk"),
                                    new Complex(new ChildId("JD", 26), "Dorian"),
                                    new Complex(new ChildId("Carla", 29), "Espinosa"));
        getDs().save(list);

        MorphiaKeyIterator<Complex> keys = getDs().createQuery(Complex.class).fetchKeys();
        assertTrue(keys.hasNext());
        assertEquals(list.get(0).getId(), keys.next().getId());
        assertEquals(list.get(1).getId(), keys.next().getId());
        assertEquals(list.get(2).getId(), keys.next().getId());
        assertFalse(keys.hasNext());
    }

    @Test
    public void testFetchEmptyEntities() {
        List<Complex> list = asList(new Complex(new ChildId("Turk", 27), "Turk"),
                                    new Complex(new ChildId("JD", 26), "Dorian"),
                                    new Complex(new ChildId("Carla", 29), "Espinosa"));
        getDs().save(list);

        Iterator<Complex> keys = getDs().createQuery(Complex.class).fetchEmptyEntities();
        assertTrue(keys.hasNext());
        assertEquals(list.get(0).getId(), keys.next().getId());
        assertEquals(list.get(1).getId(), keys.next().getId());
        assertEquals(list.get(2).getId(), keys.next().getId());
        assertFalse(keys.hasNext());
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
            collectionRef = asList(refs);
            lazyCollectionRef = asList(refs);
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

    private static class ComplexParent {
        @Id
        private ObjectId id;

        @Reference
        private Complex complex;

        @Reference
        private List<Complex> list = new ArrayList<Complex>();

        @Reference(lazy = true)
        private List<Complex> lazyList = new ArrayList<Complex>();

        public ObjectId getId() {
            return id;
        }

        public void setId(final ObjectId id) {
            this.id = id;
        }

        public Complex getComplex() {
            return complex;
        }

        public void setComplex(final Complex complex) {
            this.complex = complex;
        }

        public List<Complex> getList() {
            return list;
        }

        public void setList(final List<Complex> list) {
            this.list = list;
        }

        public List<Complex> getLazyList() {
            return lazyList;
        }

        public void setLazyList(final List<Complex> lazyList) {
            this.lazyList = lazyList;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ComplexParent)) {
                return false;
            }

            final ComplexParent that = (ComplexParent) o;

            if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) {
                return false;
            }
            if (getComplex() != null ? !getComplex().equals(that.getComplex()) : that.getComplex() != null) {
                return false;
            }
            if (getList() != null ? !getList().equals(that.getList()) : that.getList() != null) {
                return false;
            }
            return getLazyList() != null ? getLazyList().equals(that.getLazyList()) : that.getLazyList() == null;

        }

        @Override
        public int hashCode() {
            int result = getId() != null ? getId().hashCode() : 0;
            result = 31 * result + (getComplex() != null ? getComplex().hashCode() : 0);
            result = 31 * result + (getList() != null ? getList().hashCode() : 0);
            result = 31 * result + (getLazyList() != null ? getLazyList().hashCode() : 0);
            return result;
        }
    }

    @Entity("complex")
    private static class Complex {
        @Id
        @Embedded
        private ChildId id;

        private String value;

        public Complex() {
        }

        public Complex(final ChildId id, final String value) {
            this.id = id;
            this.value = value;
        }

        public ChildId getId() {
            return id;
        }

        public void setId(final ChildId id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Complex)) {
                return false;
            }

            final Complex complex = (Complex) o;

            if (getId() != null ? !getId().equals(complex.getId()) : complex.getId() != null) {
                return false;
            }
            return getValue() != null ? getValue().equals(complex.getValue()) : complex.getValue() == null;

        }

        @Override
        public int hashCode() {
            int result = getId() != null ? getId().hashCode() : 0;
            result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
            return result;
        }
    }

    @Embedded
    private static class ChildId {
        private String name;
        private int age;

        public ChildId() {
        }

        public ChildId(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public int getAge() {
            return age;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ChildId)) {
                return false;
            }

            final ChildId childId = (ChildId) o;

            if (getAge() != childId.getAge()) {
                return false;
            }
            return getName() != null ? getName().equals(childId.getName()) : childId.getName() == null;

        }

        @Override
        public int hashCode() {
            int result = getName() != null ? getName().hashCode() : 0;
            result = 31 * result + getAge();
            return result;
        }
    }
}
