package dev.morphia.test.mapping.experimental;

import com.mongodb.DBRef;
import com.mongodb.client.MongoCursor;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.stages.Lookup;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MapperOptions.PropertyDiscovery;
import dev.morphia.mapping.experimental.MorphiaReference;
import dev.morphia.query.FindOptions;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.Author;
import dev.morphia.test.models.Book;
import dev.morphia.test.models.FacebookUser;
import dev.morphia.test.models.TestEntity;
import dev.morphia.test.models.methods.MethodMappedFriend;
import dev.morphia.test.models.methods.MethodMappedUser;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.morphia.Morphia.createDatastore;
import static dev.morphia.aggregation.experimental.stages.Unwind.on;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.in;
import static java.util.Arrays.asList;
import static java.util.List.of;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class TestReferences extends TestBase {
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
        Assert.assertNotNull(getDs().find(Sets.class)
                                    .filter(eq("refs", ref))
                                    .first());

        final MapOfSet map = new MapOfSet();
        map.strings = new HashMap<>();
        map.strings.put("name", new TreeSet<>(asList("one", "two", "three")));
        getDs().save(map);
        final MapOfSet first = getDs().find(MapOfSet.class).first();
        Assert.assertEquals(map, first);
    }

    @Test
    public void testAggregationLookups() {
        final Author author = new Author("Jane Austen");
        getDs().save(author);

        List<Book> books = List.of(
            new Book("Sense and Sensibility", author),
            new Book("Pride and Prejudice", author),
            new Book("Mansfield Park", author),
            new Book("Emma", author),
            new Book("Northanger Abbey", author));
        getDs().save(books);

        author.setList(books);
        author.setSet(new HashSet<>(books));
        // Map<String, Book> map = addBookMap(author);

        getDs().save(author);

        Aggregation<Author> aggregation = getDs().aggregate(Author.class)
                                                 .lookup(Lookup.from(Book.class)
                                                               .as("set")
                                                               .foreignField("_id")
                                                               .localField("set"))
                                                 .lookup(Lookup.from(Book.class)
                                                               .as("list")
                                                               .foreignField("_id")
                                                               .localField("list"));

        final Author loaded = aggregation
                                  //  TODO how to fetch the values from a nested document for cross-referencing?
                                  //                                   .lookup(Lookup.from(Book.class)
                                  //                                                 .as("map")
                                  //                                                 .foreignField("_id")
                                  //                                                 .localField("map.$"))
                                  .execute(Author.class)
                                  .tryNext();

        assertListEquals(author.list, loaded.list);

        assertListEquals(author.set, loaded.set);

        //        validateMap(map, loaded);

        Book foundBook = getDs().aggregate(Book.class)
                                .lookup(Lookup.from(Author.class)
                                              .as("author")
                                              .foreignField("_id")
                                              .localField("author"))
                                .unwind(on("author"))
                                .execute(Book.class)
                                .next();
        assertTrue(foundBook.author.isResolved());
        Assert.assertEquals(author, foundBook.author.get());
    }

    @Test
    public final void testArrays() {
        ArrayOfReferences a = new ArrayOfReferences();
        final Ref ref1 = new Ref();
        final Ref ref2 = new Ref();

        a.refs[0] = ref1;
        a.refs[1] = ref2;

        getDs().save(asList(ref2, ref1, a));

        getDs().find(ArrayOfReferences.class)
               .filter(eq("_id", a.getId()))
               .first();
    }

    @Test
    public void testFetchKeys() {
        List<Complex> list = asList(new Complex(new ChildId("Turk", 27), "Turk"),
            new Complex(new ChildId("JD", 26), "Dorian"),
            new Complex(new ChildId("Carla", 29), "Espinosa"));
        getDs().save(list);

        MongoCursor<Key<Complex>> keys = getDs().find(Complex.class).keys();
        assertTrue(keys.hasNext());
        assertEquals(keys.next().getId(), list.get(0).getId());
        assertEquals(keys.next().getId(), list.get(1).getId());
        assertEquals(keys.next().getId(), list.get(2).getId());
        assertFalse(keys.hasNext());
    }

    @Test
    public void testFindByEntityReference() {
        final Ref ref = new Ref("refId");
        getDs().save(ref);

        final Container container = new Container();
        container.singleRef = ref;
        getDs().save(container);

        Assert.assertNotNull(getDs().find(Container.class)
                                    .filter(eq("singleRef", ref)).iterator(new FindOptions().limit(1))
                                    .next());
    }

    @Test
    public void testInQueryAgainstReferences() {

        Plan plan1 = getDs().save(new Plan("Trial 1"));
        Plan plan2 = getDs().save(new Plan("Trial 2"));

        getDs().save(new Org("Test Org1", plan1));
        getDs().save(new Org("Test Org2", plan2));

        long count = getDs().find(Org.class).filter(eq("name", "Test Org1")).count();
        assertEquals(count, 1);

        count = getDs().find(Org.class).filter(in("plan", of(plan1))).count();
        assertEquals(count, 1);

        count = getDs().find(Org.class).filter(in("plan", of(plan1, plan2))).count();
        assertEquals(count, 2);
    }

    @Test
    public final void testMultiDimArrayPersistence() {
        MultiDimArrayOfReferences a = new MultiDimArrayOfReferences();
        final Ref ref1 = new Ref();
        final Ref ref2 = new Ref();

        a.arrays = new Ref[][][]{
            new Ref[][]{
                new Ref[]{ref1, ref2}
            }
        };
        a.lists = List.of(List.of(List.of(ref1), List.of(ref2)));
        getDs().save(asList(ref2, ref1, a));

        assertEquals(a, getDs().find(MultiDimArrayOfReferences.class)
                               .filter(eq("_id", a.getId()))
                               .first());
    }

    @Test
    public void testReferencesWithoutMapping() {
        Child child1 = new Child();
        getDs().save(child1);

        Parent parent1 = new Parent();
        parent1.children.add(child1);
        getDs().save(parent1);

        List<Parent> parentList = getDs().find(Parent.class).iterator().toList();
        Assert.assertEquals(parentList.size(), 1);

        withOptions(MapperOptions.DEFAULT, () -> {
            Assert.assertEquals(getDs().find(Parent.class).iterator().toList().size(), 1);
        });
    }

    private static class ArrayOfReferences extends TestEntity {
        @Reference
        private final Ref[] refs = new Ref[2];
    }

    @Test
    public void testComplexIds() {
        ComplexParent parent = new ComplexParent();
        parent.complex = new Complex(new ChildId("Bob", 67), "Kelso");
        parent.list = List.of(new Complex(new ChildId("Turk", 27), "Turk"));
        parent.lazyList = List.of(new Complex(new ChildId("Bippity", 67), "Boppity"));

        getDs().save(parent.complex);
        getDs().save(parent.list);
        getDs().save(parent.lazyList);
        getDs().save(parent);

        ComplexParent loaded = getDs().find(ComplexParent.class)
                                      .filter(eq("_id", parent.id))
                                      .first();
        assertEquals(loaded, parent);
    }

    @Test
    public void testDBRefSaves() {
        getMapper().map(List.of(FacebookUser.class));

        FacebookUser tom = new FacebookUser(1, "Tom Anderson");
        tom.friends.addAll(List.of(new FacebookUser(2, "Cameron Winklevoss"), new FacebookUser(3, "Tyler Winklevoss")));
        getDs().save(tom.friends);
        getDs().save(tom);

        Document loaded = getMapper().getCollection(FacebookUser.class)
                                     .withDocumentClass(Document.class)
                                     .find(new Document("_id", 1))
                                     .first();
        ((List<Object>) loaded.get("friends"))
            .forEach(f -> assertEquals(f.getClass(), DBRef.class));
    }

    @Test
    public void testIdOnly() {
        getMapper().map(List.of(HasIdOnly.class, FacebookUser.class));

        HasIdOnly tom = new HasIdOnly();
        tom.list.addAll(List.of(new FacebookUser(2, "Cameron Winklevoss"), new FacebookUser(3, "Tyler Winklevoss")));
        getDs().save(tom.list);
        getDs().save(tom);

        Document loaded = getMapper().getCollection(HasIdOnly.class)
                                     .withDocumentClass(Document.class)
                                     .find(new Document())
                                     .first();
        ((List<Object>) loaded.get("list"))
            .forEach(f -> assertEquals(f.getClass(), Long.class));
    }

    @Entity(value = "children", useDiscriminator = false)
    private static class Child {
        @Id
        private ObjectId id;

    }

    @Test
    public void testMethodMapping() {
        Datastore datastore = createDatastore(getMongoClient(), TEST_DB_NAME,
            MapperOptions.builder()
                         .propertyDiscovery(
                             PropertyDiscovery.METHODS)
                         .build());

        datastore.getMapper().map(MethodMappedUser.class);
        MethodMappedUser user = new MethodMappedUser();
        MethodMappedFriend friend = new MethodMappedFriend();
        user.setFriend(friend);
        user.setFriends(MorphiaReference.wrap(List.of(friend)));

        datastore.save(List.of(friend, user));

        MethodMappedUser loaded = datastore.find(MethodMappedUser.class).first();
        assertFalse(loaded.getFriends().isResolved());
        assertEquals(loaded.getFriend(), friend);
        assertEquals(loaded.getFriends().get().get(0), friend);
        assertEquals(loaded, user);
    }

    @Test
    public void testMultipleDatabasesSingleThreaded() {
        getMapper().map(List.of(FacebookUser.class));

        final Datastore ds1 = createDatastore(getMongoClient(), "db1");
        final Datastore ds2 = createDatastore(getMongoClient(), "db2");

        final FacebookUser db1Friend = new FacebookUser(3, "DB1 FaceBook Friend");
        ds1.save(db1Friend);
        final FacebookUser db1User = new FacebookUser(1, "DB1 FaceBook User");
        db1User.friends.add(db1Friend);
        ds1.save(db1User);

        final FacebookUser db2Friend = new FacebookUser(4, "DB2 FaceBook Friend");
        ds2.save(db2Friend);
        final FacebookUser db2User = new FacebookUser(2, "DB2 FaceBook User");
        db2User.friends.add(db2Friend);
        ds2.save(db2User);

        testFirstDatastore(ds1);
        testSecondDatastore(ds2);

        for (int i = 1; i <= 4; i++) {
            assertNull(getDs().find(FacebookUser.class).filter(eq("id", i)).first());
        }
    }

    private void testFirstDatastore(Datastore datastore) {
        final FacebookUser user = datastore.find(FacebookUser.class).filter(eq("id", 1)).first();
        assertNotNull(user);
        assertNotNull(datastore.find(FacebookUser.class).filter(eq("id", 3)).first());

        assertEquals(user.friends.size(), 1, "Should find 1 friend");
        assertEquals(user.friends.get(0).id, 3, "Should find the right friend");

        assertNull(datastore.find(FacebookUser.class).filter(eq("id", 2)).first());
        assertNull(datastore.find(FacebookUser.class).filter(eq("id", 4)).first());
    }

    private void testSecondDatastore(Datastore datastore) {
        assertNull(datastore.find(FacebookUser.class).filter(eq("id", 1)).first());
        assertNull(datastore.find(FacebookUser.class).filter(eq("id", 3)).first());

        final FacebookUser db2FoundUser = datastore.find(FacebookUser.class).filter(eq("id", 2)).first();
        assertNotNull(db2FoundUser);
        assertNotNull(datastore.find(FacebookUser.class).filter(eq("id", 4)).first());
        assertEquals(db2FoundUser.friends.size(), 1, "Should find 1 friend");
        assertEquals(db2FoundUser.friends.get(0).id, 4, "Should find the right friend");
    }

    @Entity
    private static class Container {
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

        Container(List<Ref> refs) {
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

        List<Ref> getCollectionRef() {
            return collectionRef;
        }

        ObjectId getId() {
            return id;
        }

        List<Ref> getLazyCollectionRef() {
            return lazyCollectionRef;
        }

        LinkedHashMap<Integer, Ref> getLazyMapRef() {
            return lazyMapRef;
        }

        Ref getLazySingleRef() {
            return lazySingleRef;
        }

        LinkedHashMap<Integer, Ref> getMapRef() {
            return mapRef;
        }

        Ref getSingleRef() {
            return singleRef;
        }
    }

    @Test(groups = "references")
    public void testReference() {
        getMapper().map(CompoundIdEntity.class, CompoundId.class);

        final CompoundIdEntity sibling = new CompoundIdEntity();
        sibling.id = new CompoundId("sibling ID");
        getDs().save(sibling);

        final CompoundIdEntity entity = new CompoundIdEntity();
        entity.id = new CompoundId("entity ID");
        entity.e = "some value";
        entity.sibling = sibling;
        getDs().save(entity);

        Assert.assertEquals(entity, getDs().find(entity.getClass()).filter(eq("_id", entity.id)).first());
    }

    @Entity
    private static class CompoundId {
        private final ObjectId id = new ObjectId();
        private String name;

        CompoundId() {
        }

        CompoundId(String n) {
            name = n;
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CompoundId)) {
                return false;
            }
            final CompoundId other = ((CompoundId) obj);
            return other.id.equals(id) && other.name.equals(name);
        }

    }

    @Entity
    private static class CompoundIdEntity {
        @Id
        private CompoundId id;
        private String e;
        @Reference
        private CompoundIdEntity sibling;

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + (e != null ? e.hashCode() : 0);
            result = 31 * result + (sibling != null ? sibling.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final CompoundIdEntity that = (CompoundIdEntity) o;

            if (!id.equals(that.id)) {
                return false;
            }
            if (e != null ? !e.equals(that.e) : that.e != null) {
                return false;
            }
            return !(sibling != null ? !sibling.equals(that.sibling) : that.sibling != null);

        }
    }

    @Embedded
    public static class ChildId {
        private String name;
        private int age;

        ChildId() {
        }

        public ChildId(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            int result = getName() != null ? getName().hashCode() : 0;
            result = 31 * result + getAge();
            return result;
        }

        @Override
        public boolean equals(Object o) {
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

        int getAge() {
            return age;
        }
    }

    @Entity("complex")
    public static class Complex {
        @Id
        private ChildId id;

        private String value;

        Complex() {
        }

        public Complex(ChildId id, String value) {
            this.id = id;
            this.value = value;
        }

        public ChildId getId() {
            return id;
        }

        public void setId(ChildId id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            int result = getId() != null ? getId().hashCode() : 0;
            result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
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
    }

    @Entity
    private static class ComplexParent {
        @Id
        private ObjectId id;

        @Reference
        private Complex complex;

        @Reference
        private List<Complex> list;

        @Reference(lazy = true)
        private List<Complex> lazyList;

        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        public List<Complex> getList() {
            return list;
        }

        public void setList(List<Complex> list) {
            this.list = list;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, complex, list, lazyList);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ComplexParent)) {
                return false;
            }
            ComplexParent that = (ComplexParent) o;
            AtomicBoolean equals = new AtomicBoolean(Objects.equals(id, that.id) &&
                                                     Objects.equals(complex, that.complex) &&
                                                     Objects.equals(list, that.list));

            assertEquals(lazyList.size(), that.lazyList.size());
            lazyList.forEach(
                d -> equals.set(equals.get() && that.lazyList.contains(d)));

            return equals.get();
        }

        Complex getComplex() {
            return complex;
        }

        public void setComplex(Complex complex) {
            this.complex = complex;
        }

        List<Complex> getLazyList() {
            return lazyList;
        }

        public void setLazyList(List<Complex> lazyList) {
            this.lazyList = lazyList;
        }
    }

    @Entity(value = "as", useDiscriminator = false)
    private static class HasIdOnly {
        @Reference(idOnly = true)
        private final List<FacebookUser> list = new ArrayList<>();
        @Id
        private ObjectId id;
        private String name;
    }

    @Entity("cs")
    public static class MapOfSet {
        @Id
        private ObjectId id;

        private Map<String, Set<String>> strings;

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (strings != null ? strings.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
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
    }

    @Entity(useDiscriminator = false)
    private static class Org {
        @Id
        private ObjectId id;
        @Property("name")
        private String name;
        @Reference("plan")
        private Plan plan;

        public Org(String name, Plan plan) {
            this.name = name;
            this.plan = plan;
        }

        public Org() {
        }
    }

    @Entity(useDiscriminator = false)
    private static class Plan {

        @Id
        private ObjectId id;
        @Property("name")
        private String name;

        public Plan() {
        }

        public Plan(String name) {
            this.name = name;
        }
    }

    private static class MultiDimArrayOfReferences extends TestEntity {
        @Reference(idOnly = true)
        private Ref[][][] arrays;
        private List<List<List<Ref>>> lists;

        @Override
        public int hashCode() {
            int result = Arrays.deepHashCode(arrays);
            result = 31 * result + (lists != null ? lists.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MultiDimArrayOfReferences)) {
                return false;
            }

            final MultiDimArrayOfReferences that = (MultiDimArrayOfReferences) o;

            if (!Arrays.deepEquals(arrays, that.arrays)) {
                return false;
            }
            return lists != null ? lists.equals(that.lists) : that.lists == null;
        }
    }

    @Entity(value = "parents", useDiscriminator = false)
    private static class Parent {

        @Reference(lazy = true)
        private final List<Child> children = new ArrayList<>();
        @Id
        private ObjectId id;

    }

    @Entity
    private static class Ref {
        @Id
        private String id;

        public Ref() {
        }

        Ref(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return getId() != null ? getId().hashCode() : 0;
        }

        @Override
        public boolean equals(Object o) {
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

}
