package dev.morphia.test.mapping.experimental;

import com.mongodb.DBRef;
import com.mongodb.client.MongoCursor;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IdGetter;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MapperOptions.PropertyDiscovery;
import dev.morphia.mapping.experimental.MorphiaReference;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.query.FindOptions;
import dev.morphia.test.mapping.ProxyTestBase;
import dev.morphia.test.models.Author;
import dev.morphia.test.models.Book;
import dev.morphia.test.models.FacebookUser;
import dev.morphia.test.models.TestEntity;
import dev.morphia.test.models.methods.MethodMappedFriend;
import dev.morphia.test.models.methods.MethodMappedUser;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.annotations.Ignore;
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
import static dev.morphia.aggregation.experimental.stages.Lookup.lookup;
import static dev.morphia.aggregation.experimental.stages.Unwind.unwind;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.in;
import static java.util.Arrays.asList;
import static java.util.List.of;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

@SuppressWarnings({"MismatchedReadAndWriteOfArray", "unchecked", "removal"})
public class TestReferences extends ProxyTestBase {

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
        assertNotNull(getDs().find(Sets.class)
                             .filter(eq("refs", ref))
                             .first());

        final MapOfSet map = new MapOfSet();
        map.strings = new HashMap<>();
        map.strings.put("name", new TreeSet<>(asList("one", "two", "three")));
        getDs().save(map);
        final MapOfSet first = getDs().find(MapOfSet.class).first();
        assertEquals(map, first);
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
                                                 .lookup(lookup(Book.class)
                                                     .as("set")
                                                     .foreignField("_id")
                                                     .localField("set"))
                                                 .lookup(lookup(Book.class)
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
                                .lookup(lookup(Author.class)
                                    .as("author")
                                    .foreignField("_id")
                                    .localField("author"))
                                .unwind(unwind("author"))
                                .execute(Book.class)
                                .next();
        assertTrue(foundBook.author.isResolved());
        assertEquals(author, foundBook.author.get());
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
    public final void testCallIdGetterWithoutFetching() {
        checkForProxyTypes();

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();
        getDs().save(reference);
        ObjectId id = reference.getId();

        root.r = reference;
        reference.setFoo("bar");
        getDs().save(root);

        root = getDs().find(RootEntity.class)
                      .filter(eq("_id", root.getId()))
                      .first();

        final ReferencedEntity p = root.r;

        assertIsProxy(p);
        assertNotFetched(p);

        ObjectId idFromProxy = p.getId();
        assertEquals(id, idFromProxy);

        // Since getId() is annotated with @IdGetter, it should not cause the
        // referenced entity to be fetched
        assertNotFetched(p);

        p.getFoo();

        // Calling getFoo() should have caused the referenced entity to be fetched
        assertFetched(p);

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

        assertNotNull(getDs().find(Container.class)
                             .filter(eq("singleRef", ref)).iterator(new FindOptions().limit(1))
                             .next());
    }

    @Test
    public final void testGetKeyWithoutFetching() {
        checkForProxyTypes();

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();

        root.r = reference;
        reference.setFoo("bar");

        final ObjectId id = getDs().save(reference).getId();
        getDs().save(root);

        RootEntity loaded = getDs().find(RootEntity.class)
                                   .filter(eq("_id", root.getId()))
                                   .first();

        final ReferencedEntity p = loaded.r;

        assertIsProxy(p);
        assertNotFetched(p);
        p.getFoo();
        // should be fetched now.
        assertFetched(p);

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
    public void testLazyWithParent() {
        Datastore datastore = getDs();
        datastore.getMapper().map(Entity1.class, Entity2.class, EntityBase.class);

        Entity1 entity1 = new Entity1("entity1");
        datastore.save(List.of(entity1, new Entity2("entity2", entity1)));

        var entities = datastore.find(Entity2.class).iterator().toList();

        assertNotNull(entities.get(0));
        Entity1 reference = entities.get(0).getReference();
        assertNotNull(reference);
        //        System.out.println("reference = " + reference);
        assertEquals(reference.getName(), "entity1", "name should cause a fetch");
        assertNotNull(reference.getId(), "ID shouldn't be null");
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

    @Test(expectedExceptions = ReferenceException.class)
    public void testMissingRef() {
        final Source source = new Source();
        source.setTarget(new Target());

        getDs().save(source);

        getDs().find(Source.class).iterator().toList();
    }

    @Test(expectedExceptions = ReferenceException.class)
    public void testMissingRefLazy() {
        final Source e = new Source();
        e.setLazy(new Target());

        getDs().save(e);
        Source source = getDs().find(Source.class).first();
        assertNull(source.getLazy().getFoo());
    }

    @Test(expectedExceptions = ReferenceException.class)
    public void testMissingRefLazyIgnoreMissing() {
        final Source e = new Source();
        e.setIgnoreMissing(new Target());

        getDs().save(e); // does not fail due to pre-initialized Ids

        Source source = getDs().find(Source.class).first();
        source.getIgnoreMissing().getFoo();
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

        assertEquals(entity, getDs().find(entity.getClass()).filter(eq("_id", entity.id)).first());
    }

    @Test
    public void testReferencesWithoutMapping() {
        Child child1 = new Child();
        getDs().save(child1);

        Parent parent1 = new Parent();
        parent1.children.add(child1);
        getDs().save(parent1);

        List<Parent> parentList = getDs().find(Parent.class).iterator().toList();
        assertEquals(parentList.size(), 1);

        withOptions(MapperOptions.DEFAULT, () -> {
            assertEquals(getDs().find(Parent.class).iterator().toList().size(), 1);
        });
    }

    @Test
    @Ignore("entity caching needs to be implemented")
    public final void testSameProxy() {
        checkForProxyTypes();

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();

        root.r = reference;
        root.secondReference = reference;
        reference.setFoo("bar");

        getDs().save(reference);
        getDs().save(root);

        root = getDs().find(RootEntity.class)
                      .filter(eq("_id", root.getId()))
                      .first();
        assertSame(root.r, root.secondReference);
    }

    @Test
    public final void testShortcutInterface() {
        checkForProxyTypes();

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();
        final ReferencedEntity second = new ReferencedEntity();

        root.r = reference;
        root.secondReference = second;
        reference.setFoo("bar");

        getDs().save(second);
        getDs().save(reference);
        getDs().save(root);

        root = getDs().find(RootEntity.class)
                      .filter(eq("_id", root.getId()))
                      .first();

        ReferencedEntity referenced = root.r;

        assertIsProxy(referenced);
        assertNotFetched(referenced);
        assertNotFetched(root.secondReference);
        referenced.getFoo();
        // should be fetched now.
        assertFetched(referenced);
        assertNotFetched(root.secondReference);
        root.secondReference.getFoo();
        assertFetched(root.secondReference);

        root = getDs().find(RootEntity.class)
                      .filter(eq("_id", root.getId()))
                      .first();
        assertNotFetched(root.r);
        assertNotFetched(root.secondReference);
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

    private static class ArrayOfReferences extends TestEntity {
        @Reference
        private final Ref[] refs = new Ref[2];
    }

    @Entity(value = "children", useDiscriminator = false)
    private static class Child {
        @Id
        private ObjectId id;

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

    @Entity
    public static class Entity1 extends EntityBase {
        private String name;

        public Entity1() {
        }

        public Entity1(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Entity
    public static class Entity2 extends EntityBase {
        private String anotherName;
        @Reference(idOnly = true, lazy = true)
        private Entity1 reference;

        public Entity2() {
        }

        public Entity2(String anotherName, Entity1 reference) {
            this.anotherName = anotherName;
            this.reference = reference;
        }

        public String getAnotherName() {
            return anotherName;
        }

        public void setAnotherName(String anotherName) {
            this.anotherName = anotherName;
        }

        public Entity1 getReference() {
            return reference;
        }

        public void setReference(Entity1 reference) {
            this.reference = reference;
        }
    }

    @Entity
    public static abstract class EntityBase {
        @Id
        protected ObjectId id;

        @IdGetter
        public ObjectId getId() {
            return id;
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

    @Entity(value = "parents", useDiscriminator = false)
    private static class Parent {

        @Reference(lazy = true)
        private final List<Child> children = new ArrayList<>();
        @Id
        private ObjectId id;

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

    public static class ReferencedEntity extends TestEntity {
        private String foo;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String string) {
            foo = string;
        }

        @Override
        @IdGetter
        public ObjectId getId() {
            return super.getId();
        }

        @Override
        public int hashCode() {
            return getFoo() != null ? getFoo().hashCode() : 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ReferencedEntity)) {
                return false;
            }

            final ReferencedEntity that = (ReferencedEntity) o;

            return getFoo() != null ? getFoo().equals(that.getFoo()) : that.getFoo() == null;
        }
    }

    private static class RootEntity extends TestEntity {
        @Reference(lazy = true)
        private ReferencedEntity r;
        @Reference(lazy = true)
        private ReferencedEntity secondReference;

    }

    @Entity("sets")
    public static class Sets {
        @Id
        private ObjectId id;

        @Reference
        private Set<Ref> refs;
    }

    @Entity
    static class Source {
        @Id
        private final ObjectId id = new ObjectId();
        @Reference
        private Target target;
        @Reference(lazy = true)
        private Target lazy;
        @Reference(lazy = true, ignoreMissing = true)
        private Target ignoreMissing;

        public Target getIgnoreMissing() {
            return ignoreMissing;
        }

        public void setIgnoreMissing(Target ignoreMissing) {
            this.ignoreMissing = ignoreMissing;
        }

        public Target getLazy() {
            return lazy;
        }

        public void setLazy(Target lazy) {
            this.lazy = lazy;
        }

        public Target getTarget() {
            return target;
        }

        public void setTarget(Target target) {
            this.target = target;
        }
    }

    @Entity
    public static class Target {
        @Id
        private ObjectId id = new ObjectId();
        private String foo = "bar";

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }
    }
}
