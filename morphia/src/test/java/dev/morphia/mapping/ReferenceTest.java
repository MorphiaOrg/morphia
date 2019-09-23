package dev.morphia.mapping;

import com.mongodb.BasicDBList;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.Morphia;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.lazy.ProxyTestBase;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static dev.morphia.mapping.lazy.LazyFeatureDependencies.testDependencyFullFilled;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Category(Reference.class)
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

        ComplexParent complexParent = getDs().find(ComplexParent.class).filter("_id", parent.id).first();
        assertEquals(parent, complexParent);
    }

    @Test
    public void testFindByEntityReference() {
        final Ref ref = new Ref("refId");
        getDs().save(ref);

        final Container container = new Container();
        container.singleRef = ref;
        getDs().save(container);

        Assert.assertNotNull(getDs().find(Container.class).filter("singleRef", ref)
                                    .execute(new FindOptions().limit(1))
                                    .next());
    }

    @Test
    public void testIdOnlyReferences() {
        final List<Ref> refs = asList(new Ref("foo"), new Ref("bar"), new Ref("baz"));
        final Container c = new Container(refs);

        // test that we can save it
        final ObjectId key = getDs().save(c).getId();
        getDs().save(refs);

        // ensure that we're not using DBRef
        final MongoCollection<Document> collection = getDatabase().getCollection(Container.class.getSimpleName());
        final Document persisted = collection.find(new Document("_id", key)).first();
        assertNotNull(persisted);
        assertEquals("foo", persisted.get("singleRef"));
        assertEquals("foo", persisted.get("lazySingleRef"));

        final List<String> expectedList = new ArrayList<>();
        expectedList.add("foo");
        expectedList.add("bar");
        expectedList.add("baz");
        assertEquals(expectedList, persisted.get("collectionRef"));
        assertEquals(expectedList, persisted.get("lazyCollectionRef"));

        final Document expectedMap = new Document();
        expectedMap.put("0", "foo");
        expectedMap.put("1", "bar");
        expectedMap.put("2", "baz");
        assertEquals(expectedMap, persisted.get("mapRef"));
        assertEquals(expectedMap, persisted.get("lazyMapRef"));

        // ensure that we can retrieve it
        final Container retrieved = getDs().find(Container.class)
                                           .filter("_id", key)
                                           .first();

        assertEquals(refs.get(0), retrieved.getSingleRef());
        if (testDependencyFullFilled()) {
            assertIsProxy(retrieved.getLazySingleRef());
        }
        assertEquals(refs.get(0), retrieved.getLazySingleRef());

        final List<Ref> expectedRefList = new ArrayList<>();
        final Map<Integer, Ref> expectedRefMap = new LinkedHashMap<>();

        for (int i = 0; i < refs.size(); i++) {
            expectedRefList.add(refs.get(i));
            expectedRefMap.put(i, refs.get(i));
        }

        assertEquals(expectedRefList, retrieved.getCollectionRef());
        assertEquals(expectedRefList, retrieved.getLazyCollectionRef());
        assertEquals(expectedRefMap, retrieved.getMapRef());
        assertEquals(expectedRefMap, retrieved.getLazyMapRef());
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

        MapperOptions options = MapperOptions.builder(getMapper().getOptions())
                                             .storeNulls(true)
                                             .build();
        Datastore datastore = Morphia.createDatastore(getMongoClient(), getDatabase().getName(), options);

        datastore.save(container);
        allNull(container);

        options = MapperOptions.builder(getMapper().getOptions())
                               .storeNulls(true)
                               .build();
        datastore = Morphia.createDatastore(getMongoClient(), getDatabase().getName(), options);
        datastore.save(container);
        allNull(container);
    }

    @Test
    public void testReferenceQueryWithoutValidation() {
        Ref ref = new Ref("no validation");
        getDs().save(ref);
        final Container container = new Container(singletonList(ref));
        getDs().save(container);
        final Query<Container> query = getDs().find(Container.class)
                                              .disableValidation()
                                              .field("singleRef").equal(ref);
        Assert.assertNotNull(query.execute(new FindOptions().limit(1)).next());
    }

    @Test
    public void testReferencesWithoutMapping() {
        Child child1 = new Child();
        getDs().save(child1);

        Parent parent1 = new Parent();
        parent1.children.add(child1);
        getDs().save(parent1);

        List<Parent> parentList = getDs().find(Parent.class).execute().toList();
        Assert.assertEquals(1, parentList.size());

        // reset Datastore to reset internal Mapper cache, so Child class
        // already cached by previous save is cleared
        Datastore localDs = Morphia.createDatastore(getMongoClient(), getDatabase().getName());

        parentList = localDs.find(Parent.class).execute().toList();
        Assert.assertEquals(1, parentList.size());
    }

    @Test
    public void testFetchKeys() {
        List<Complex> list = asList(new Complex(new ChildId("Turk", 27), "Turk"),
            new Complex(new ChildId("JD", 26), "Dorian"),
            new Complex(new ChildId("Carla", 29), "Espinosa"));
        getDs().save(list);

        MongoCursor<Key<Complex>> keys = getDs().find(Complex.class).keys();
        assertTrue(keys.hasNext());
        assertEquals(list.get(0).getId(), keys.next().getId());
        assertEquals(list.get(1).getId(), keys.next().getId());
        assertEquals(list.get(2).getId(), keys.next().getId());
        assertFalse(keys.hasNext());
    }

    @Test
    public void maps() {
        Ref ref = new Ref("refId");
        getDs().save(ref);
        // create entity B with a reference to A
        Sets sets = new Sets();
        sets.refs = new HashSet<>();
        sets.refs.add(ref);
        getDs().save(sets);

        // this query throws a NullPointerException
        Assert.assertNotNull(getDs().find(Sets.class).filter("refs", ref).first());

        final MapOfSet map = new MapOfSet();
        map.strings = new HashMap<>();
        map.strings.put("name", new TreeSet<>(asList("one", "two", "three")));
        getDs().save(map);
        final MapOfSet first = getDs().find(MapOfSet.class).first();
        Assert.assertEquals(map, first);
    }

    private void allNull(final Container container) {
        Assert.assertNull(container.lazyMapRef);
        Assert.assertNull(container.singleRef);
        Assert.assertNull(container.lazySingleRef);
        Assert.assertNull(container.collectionRef);
        Assert.assertNull(container.lazyCollectionRef);
        Assert.assertNull(container.mapRef);
    }

    @Entity
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
        Container() {
        }

        Container(final List<Ref> refs) {
            singleRef = refs.get(0);
            lazySingleRef = refs.get(0);
            collectionRef = refs;
            lazyCollectionRef = refs;
            mapRef = new LinkedHashMap<>();
            lazyMapRef = new LinkedHashMap<>();

            for (int i = 0; i < refs.size(); i++) {
                mapRef.put(i, refs.get(i));
                lazyMapRef.put(i, refs.get(i));
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

        Ref(final String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
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

            return getId() != null ? getId().equals(ref.getId()) : ref.getId() == null;
        }

        @Override
        public int hashCode() {
            return getId() != null ? getId().hashCode() : 0;
        }

        @Override
        public String toString() {
            return String.format("Ref{id='%s'}", id);
        }
    }

    @Entity("sets")
    public static class Sets {

        @Id
        private ObjectId id;

        @Reference
        private Set<Ref> refs;
    }

    @Entity("cs")
    public static class MapOfSet {
        @Id
        private ObjectId id;

        private Map<String, Set<String>> strings;

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final MapOfSet map = (MapOfSet) o;

            if (id != null ? !id.equals(map.id) : map.id != null) {
                return false;
            }
            return strings != null ? strings.equals(map.strings) : map.strings == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (strings != null ? strings.hashCode() : 0);
            return result;
        }
    }

    @Entity(value = "children", useDiscriminator = false)
    static class Child {
        @Id
        private ObjectId id;

    }

    @Entity(value = "parents", useDiscriminator = false)
    private static class Parent {

        @Id
        private ObjectId id;
        @Reference(lazy = true)
        private List<Child> children = new ArrayList<>();

    }

    @Entity
    private static class ComplexParent {
        @Id
        private ObjectId id;

        @Reference
        private Complex complex;

        @Reference
        private List<Complex> list = new ArrayList<>();

        @Reference(lazy = true)
        private List<Complex> lazyList = new ArrayList<>();

        public ObjectId getId() {
            return id;
        }

        public void setId(final ObjectId id) {
            this.id = id;
        }

        Complex getComplex() {
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

        List<Complex> getLazyList() {
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
    public static class Complex {
        @Id
        private ChildId id;

        private String value;

        Complex() {
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
    public static class ChildId {
        private String name;
        private int age;

        ChildId() {
        }

        public ChildId(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        int getAge() {
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
