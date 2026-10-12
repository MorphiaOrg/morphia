package dev.morphia.test.mapping;

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

import com.mongodb.DBRef;
import com.mongodb.client.model.ReturnDocument;

import dev.morphia.Datastore;
import dev.morphia.ModifyOptions;
import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.Aggregation;
import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IdGetter;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.PropertyDiscovery;
import dev.morphia.mapping.codec.DecodeSession;
import dev.morphia.mapping.codec.references.MorphiaProxy;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.query.Sort;
import dev.morphia.test.models.Author;
import dev.morphia.test.models.Book;
import dev.morphia.test.models.FacebookUser;
import dev.morphia.test.models.TestEntity;
import dev.morphia.test.models.methods.MethodMappedFriend;
import dev.morphia.test.models.methods.MethodMappedUser;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static dev.morphia.Morphia.createDatastore;
import static dev.morphia.aggregation.stages.Lookup.lookup;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.in;
import static dev.morphia.query.updates.UpdateOperators.setOnInsert;
import static java.util.Arrays.asList;
import static java.util.List.of;

@SuppressWarnings("removal")
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
        Assertions.assertNotNull(getDs().find(Sets.class)
                .filter(eq("refs", ref))
                .first());

        final MapOfSet map = new MapOfSet();
        map.strings = new HashMap<>();
        map.strings.put("name", new TreeSet<>(asList("one", "two", "three")));
        getDs().save(map);
        final MapOfSet first = getDs().find(MapOfSet.class).first();
        Assertions.assertEquals(first, map);
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
                .pipeline(lookup(Book.class)
                        .as("set")
                        .foreignField("_id")
                        .localField("set"),
                        lookup(Book.class)
                                .as("list")
                                .foreignField("_id")
                                .localField("list"));

        final Author loaded = aggregation
                //  TODO how to fetch the values from a nested document for cross-referencing?
                //                                   .lookup(Lookup.from(Book.class)
                //                                                 .as("map")
                //                                                 .foreignField("_id")
                //                                                 .localField("map.$"))
                .iterator()
                .tryNext();

        assertListEquals(author.list, loaded.list);

        assertListEquals(author.set, loaded.set);
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
        Assertions.assertEquals(idFromProxy, id);

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
        Assertions.assertEquals(parent, loaded);
    }

    @Test
    public void testDBRefSaves() {
        FacebookUser tom = new FacebookUser(1, "Tom Anderson");
        tom.friends.addAll(List.of(new FacebookUser(2, "Cameron Winklevoss"), new FacebookUser(3, "Tyler Winklevoss")));
        getDs().save(tom.friends);
        getDs().save(tom);

        Document loaded = getDs().getCollection(FacebookUser.class)
                .withDocumentClass(Document.class)
                .find(new Document("_id", 1))
                .first();
        ((List<Object>) loaded.get("friends"))
                .forEach(f -> Assertions.assertEquals(DBRef.class, f.getClass()));
    }

    @Test
    public void testEqWithNull() {
        getDs().find(Container.class)
                .filter(eq("singleRef", null))
                .iterator();
    }

    @Test
    public void testFindByEntityReference() {
        final Ref ref = new Ref("refId");
        getDs().save(ref);

        final Container container = new Container();
        container.singleRef = ref;
        getDs().save(container);

        Assertions.assertNotNull(getDs().find(Container.class)
                .filter(eq("singleRef", ref)).iterator()
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
        HasIdOnly tom = new HasIdOnly();
        tom.list.addAll(List.of(new FacebookUser(2, "Cameron Winklevoss"), new FacebookUser(3, "Tyler Winklevoss")));
        getDs().save(tom.list);
        getDs().save(tom);

        Document loaded = getDs().getCollection(HasIdOnly.class)
                .withDocumentClass(Document.class)
                .find(new Document())
                .first();
        ((List<Object>) loaded.get("list"))
                .forEach(f -> Assertions.assertEquals(Long.class, f.getClass()));
    }

    @Test
    public void testInQueryAgainstReferences() {

        Plan plan1 = getDs().save(new Plan("Trial 1"));
        Plan plan2 = getDs().save(new Plan("Trial 2"));

        getDs().save(new Org("Test Org1", plan1));
        getDs().save(new Org("Test Org2", plan2));

        long count = getDs().find(Org.class).filter(eq("name", "Test Org1")).count();
        Assertions.assertEquals(1, count);

        count = getDs().find(Org.class).filter(in("plan", of(plan1))).count();
        Assertions.assertEquals(1, count);

        count = getDs().find(Org.class).filter(in("plan", of(plan1, plan2))).count();
        Assertions.assertEquals(2, count);
    }

    @Test
    public void testLazyWithParent() {
        MorphiaDatastore datastore = getDs();
        datastore.getMapper().map(Entity1.class, Entity2.class, EntityBase.class);

        Entity1 entity1 = new Entity1("entity1");
        datastore.save(List.of(entity1, new Entity2("entity2", entity1)));

        var entities = datastore.find(Entity2.class).iterator().toList();

        Assertions.assertNotNull(entities.get(0));
        Entity1 reference = entities.get(0).getReference();
        Assertions.assertNotNull(reference);
        Assertions.assertEquals("entity1", reference.getName(), "name should cause a fetch");
        Assertions.assertNotNull(reference.getId(), "ID shouldn't be null");
    }

    @Test
    public void testMethodMapping() {
        withConfig(buildConfig()
                .propertyDiscovery(PropertyDiscovery.METHODS), () -> {

                    getDs().getMapper().map(MethodMappedUser.class);
                    MethodMappedUser user = new MethodMappedUser();
                    MethodMappedFriend friend = new MethodMappedFriend();
                    user.setFriend(friend);
                    user.setFriends(List.of(friend));

                    getDs().save(List.of(friend, user));

                    MethodMappedUser loaded = getDs().find(MethodMappedUser.class).first();
                    Assertions.assertNotNull(loaded.getFriends());
                    Assertions.assertEquals(friend, loaded.getFriend());
                    Assertions.assertEquals(friend, loaded.getFriends().get(0));
                    Assertions.assertEquals(user, loaded);
                });
    }

    @Test
    public void testMissingRef() {
        Assertions.assertThrows(ReferenceException.class, () -> {
            final Source source = new Source();
            source.setTarget(new Target());

            getDs().save(source);

            getDs().find(Source.class).iterator().toList();
        });
    }

    @Test
    public void testMissingRefLazy() {
        Assertions.assertThrows(ReferenceException.class, () -> {
            final Source e = new Source();
            e.setLazy(new Target());

            getDs().save(e);
            Source source = getDs().find(Source.class).first();
            Assertions.assertNull(source.getLazy().getFoo());
        });
    }

    @Test
    public void testMissingRefLazyIgnoreMissing() {
        Assertions.assertThrows(ReferenceException.class, () -> {
            final Source e = new Source();
            e.setIgnoreMissing(new Target());

            getDs().save(e); // does not fail due to pre-initialized Ids

            Source source = getDs().find(Source.class).first();
            source.getIgnoreMissing().getFoo();
        });
    }

    @Test
    public final void testMultiDimArrayPersistence() {
        MultiDimArrayOfReferences a = new MultiDimArrayOfReferences();
        final Ref ref1 = new Ref();
        final Ref ref2 = new Ref();

        a.arrays = new Ref[][][] {
                new Ref[][] {
                        new Ref[] { ref1, ref2 }
                }
        };
        a.lists = List.of(List.of(List.of(ref1), List.of(ref2)));
        getDs().save(asList(ref2, ref1, a));

        Assertions.assertEquals(getDs().find(MultiDimArrayOfReferences.class)
                .filter(eq("_id", a.getId()))
                .first(), a);
    }

    @Test
    public void testMultipleDatabasesSingleThreaded() {
        final Datastore ds1 = createDatastore(getMongoClient(), MorphiaConfig.load().database("db1"));
        final Datastore ds2 = createDatastore(getMongoClient(), MorphiaConfig.load().database("db2"));

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
            Assertions.assertNull(getDs().find(FacebookUser.class).filter(eq("id", i)).first());
        }
        getMongoClient().getDatabase("db1").drop();
        getMongoClient().getDatabase("db2").drop();
    }

    private void testFirstDatastore(Datastore datastore) {
        final FacebookUser user = datastore.find(FacebookUser.class).filter(eq("id", 1)).first();
        Assertions.assertNotNull(user);
        Assertions.assertNotNull(datastore.find(FacebookUser.class).filter(eq("id", 3)).first());

        Assertions.assertEquals(1, user.friends.size(), "Should find 1 friend");
        Assertions.assertEquals(3, user.friends.get(0).id, "Should find the right friend");

        Assertions.assertNull(datastore.find(FacebookUser.class).filter(eq("id", 2)).first());
        Assertions.assertNull(datastore.find(FacebookUser.class).filter(eq("id", 4)).first());
    }

    private void testSecondDatastore(Datastore datastore) {
        Assertions.assertNull(datastore.find(FacebookUser.class).filter(eq("id", 1)).first());
        Assertions.assertNull(datastore.find(FacebookUser.class).filter(eq("id", 3)).first());

        final FacebookUser db2FoundUser = datastore.find(FacebookUser.class).filter(eq("id", 2)).first();
        Assertions.assertNotNull(db2FoundUser);
        Assertions.assertNotNull(datastore.find(FacebookUser.class).filter(eq("id", 4)).first());
        Assertions.assertEquals(1, db2FoundUser.friends.size(), "Should find 1 friend");
        Assertions.assertEquals(4, db2FoundUser.friends.get(0).id, "Should find the right friend");
    }

    @Test
    @Tag("references")
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

        Assertions.assertEquals(getDs().find(entity.getClass()).filter(eq("_id", entity.id)).first(), entity);
    }

    @Test
    public void testReferencesWithoutMapping() {
        Child child1 = new Child();
        getDs().save(child1);

        Parent parent1 = new Parent();
        parent1.children.add(child1);
        getDs().save(parent1);

        List<Parent> parentList = getDs().find(Parent.class).iterator().toList();
        Assertions.assertEquals(1, parentList.size());

        withConfig(buildConfig(), () -> {
            Assertions.assertEquals(1, getDs().find(Parent.class).iterator().toList().size());
        });
    }

    @Test
    @Disabled("entity caching needs to be implemented")
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
        Assertions.assertSame(root.secondReference, root.r);
    }

    @Test
    public void testSetOnInsert() {
        getMapper().map(ParentOnInsert.class, ChildOnInsert.class);
        var child = getDs().save(new ChildOnInsert("Bob"));
        var id = new ObjectId();
        ParentOnInsert parent = getDs().find(ParentOnInsert.class)
                .disableValidation()
                .filter(eq("_id", id))
                .modify(new ModifyOptions()
                        .returnDocument(ReturnDocument.AFTER)
                        .upsert(true),
                        setOnInsert(Map.of("unique", id, "child", child)));

        Assertions.assertEquals(id.toString(), parent.unique);
        Assertions.assertEquals(child.name, parent.child.name);
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

    private static class ArrayOfReferences extends TestEntity {
        @Reference
        private final Ref[] refs = new Ref[2];
    }

    @Entity(value = "children", useDiscriminator = false)
    private static class Child {
        @Id
        private ObjectId id;

    }

    @Entity
    public static class ChildId {
        private String name;
        private int age;

        ChildId() {
        }

        public ChildId(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public int hashCode() {
            int result = getName() != null ? getName().hashCode() : 0;
            result = 31 * result + getAge();
            return result;
        }

        public String getName() {
            return name;
        }

        int getAge() {
            return age;
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
    }

    @Entity("child")
    private static class ChildOnInsert {
        @Id
        private ObjectId id;

        @Property("name")
        private String name;

        public ChildOnInsert(String name) {
            this.name = name;
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

        @Override
        public int hashCode() {
            int result = getId() != null ? getId().hashCode() : 0;
            result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
            return result;
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

            Assertions.assertEquals(that.lazyList.size(), lazyList.size());
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

    @Entity("parent")
    private static class ParentOnInsert {

        @Id
        private ObjectId id;

        @Property("unique")
        private String unique;

        @Reference("child")
        private ChildOnInsert child;

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

        @Override
        public int hashCode() {
            return getId() != null ? getId().hashCode() : 0;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
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

        @Override
        @IdGetter
        public ObjectId getId() {
            return super.getId();
        }

        @Override
        public int hashCode() {
            return getFoo() != null ? getFoo().hashCode() : 0;
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(String string) {
            foo = string;
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

    @Entity
    public static class Cached {
        @Id
        private ObjectId id = new ObjectId();
        private String name;

        public String getName() {
            return name;
        }
    }

    @Entity
    public static class CachedHolder {
        @Id
        private ObjectId id = new ObjectId();
        @Reference
        private Cached ref;
    }

    @Entity
    public static class LazyCachedHolder {
        @Id
        private ObjectId id = new ObjectId();
        @Reference(lazy = true)
        private Cached ref;
    }

    @Entity
    private static class LifecycleNodeA {
        @Id
        private ObjectId id = new ObjectId();
        private String name;
        @Reference
        private LifecycleNodeB partner;

        @PostLoad
        void postLoad() {
            loaded.incrementAndGet();
        }

        static final java.util.concurrent.atomic.AtomicInteger loaded = new java.util.concurrent.atomic.AtomicInteger();
    }

    @Entity
    private static class LifecycleNodeB {
        @Id
        private ObjectId id = new ObjectId();
        private String name;
        @Reference
        private LifecycleNodeA partner;
    }

    @Entity
    private static class TwoRefContainer {
        @Id
        private ObjectId id;
        @Reference(idOnly = true)
        private Ref ref1;
        @Reference(idOnly = true)
        private Ref ref2;
    }

    @Entity
    private static class NodeA {
        @Id
        private ObjectId id = new ObjectId();
        private String name;
        @Reference
        private NodeB partner;
    }

    @Entity
    private static class NodeB {
        @Id
        private ObjectId id = new ObjectId();
        private String name;
        @Reference
        private NodeA partner;
    }

    @Test
    public void testReferenceDeduplication() {
        // A single document with two @Reference fields pointing to the same entity.
        // Both fields should decode to the same Java instance within one decode session.
        Ref shared = new Ref("shared-ref");
        getDs().save(shared);

        TwoRefContainer container = new TwoRefContainer();
        container.ref1 = shared;
        container.ref2 = shared;
        getDs().save(container);

        TwoRefContainer loaded = getDs().find(TwoRefContainer.class).first();
        Assertions.assertNotNull(loaded);
        Assertions.assertSame(loaded.ref1, loaded.ref2, "Both ref fields should point to the same Ref instance");
    }

    /**
     * A DecodeSession must never outlive the decode that installed it. A cursor that is never closed
     * used to leave one on the thread, so a later, unrelated query resolved references out of a cache
     * that could be arbitrarily stale.
     */
    @Test
    public void testUnclosedCursorDoesNotLeakDecodeSession() {
        Cached cached = new Cached();
        cached.name = "v1";
        getDs().save(cached);
        CachedHolder holder = new CachedHolder();
        holder.ref = cached;
        getDs().save(holder);

        // deliberately not closed
        MorphiaCursor<Cached> leaked = getDs().find(Cached.class).iterator();
        Assertions.assertNotNull(leaked.next());
        Assertions.assertNull(DecodeSession.current(), "a decode session outlived the decode that installed it");

        getDs().getDatabase().getCollection(getMapper().getEntityModel(Cached.class).collectionName())
                .updateOne(new Document("_id", cached.id), new Document("$set", new Document("name", "v2")));

        CachedHolder loaded = getDs().find(CachedHolder.class).first();
        Assertions.assertNotNull(loaded);
        Assertions.assertEquals("v2", loaded.ref.name, "reference was resolved from a stale decode session");
    }

    /**
     * {@code merge()} reloads through a cursor it does not close; that must not strand a session either.
     */
    @Test
    public void testMergeDoesNotLeakDecodeSession() {
        Cached cached = new Cached();
        cached.name = "v1";
        getDs().save(cached);

        cached.name = "v2";
        getDs().merge(cached);

        Assertions.assertNull(DecodeSession.current(), "merge() left a decode session on the thread");
    }

    /**
     * Whether a reference is a proxy must not depend on what else the session happens to have cached.
     */
    @Test
    public void testLazyReferenceStaysProxyWhenCached() {
        Cached cached = new Cached();
        cached.name = "v1";
        getDs().save(cached);
        LazyCachedHolder holder = new LazyCachedHolder();
        holder.ref = cached;
        getDs().save(holder);

        // load the target first, in the same session as the holder, so it is cached when the holder decodes
        DecodeSession session = new DecodeSession();
        session.decoding(() -> {
            Cached preloaded = getDs().find(Cached.class).first();
            Assertions.assertNotNull(preloaded);

            LazyCachedHolder loaded = getDs().find(LazyCachedHolder.class).first();
            Assertions.assertNotNull(loaded);
            assertIsProxy(loaded.ref);
            assertNotFetched(loaded.ref);
            Assertions.assertEquals("v1", loaded.ref.getName());
            assertFetched(loaded.ref);
            Assertions.assertSame(preloaded, ((MorphiaProxy) loaded.ref).unwrap(),
                    "the proxy should resolve to the instance already in the session");
            return null;
        });
    }

    /**
     * Entities with lifecycle methods decode through LifecycleDecoder, which must take part in the
     * session too or cycles through them recurse forever.
     */
    @Test
    public void testCyclicReferenceWithLifecycleMethods() {
        LifecycleNodeA a = new LifecycleNodeA();
        a.name = "alpha";
        LifecycleNodeB b = new LifecycleNodeB();
        b.name = "beta";
        getDs().save(a);
        getDs().save(b);
        a.partner = b;
        b.partner = a;
        getDs().save(a);
        getDs().save(b);

        LifecycleNodeA loaded = getDs().find(LifecycleNodeA.class).filter(eq("_id", a.id)).first();
        Assertions.assertNotNull(loaded);
        Assertions.assertNotNull(loaded.partner);
        Assertions.assertEquals("beta", loaded.partner.name);
        Assertions.assertNotNull(loaded.partner.partner);
        Assertions.assertEquals("alpha", loaded.partner.partner.name);
    }

    /**
     * Deduplication still spans documents within one cursor.
     */
    @Test
    public void testReferenceDeduplicationAcrossDocuments() {
        Cached shared = new Cached();
        shared.name = "shared";
        getDs().save(shared);
        for (int i = 0; i < 3; i++) {
            CachedHolder holder = new CachedHolder();
            holder.ref = shared;
            getDs().save(holder);
        }

        List<CachedHolder> holders = getDs().find(CachedHolder.class).iterator().toList();
        Assertions.assertEquals(3, holders.size());
        Assertions.assertSame(holders.get(0).ref, holders.get(1).ref);
        Assertions.assertSame(holders.get(1).ref, holders.get(2).ref);
    }

    /**
     * The cache bound comes from configuration, and zero turns deduplication off while leaving cycle
     * safety intact.
     */
    @Test
    public void testDecodeSessionCacheSizeIsConfigurable() {
        Assertions.assertEquals(DecodeSession.DEFAULT_CACHE_SIZE,
                MorphiaConfig.load().decodeSessionCacheSize().intValue());

        DecodeSession disabled = DecodeSession.forConfig(MorphiaConfig.load().decodeSessionCacheSize(0));
        ObjectId id = new ObjectId();
        Object entity = new Cached();

        // a completed entity is not retained ...
        disabled.register("cached", id, entity);
        Assertions.assertNull(disabled.lookup("cached", id));

        // ... but one still being decoded is, so cycles still terminate
        disabled.registerInFlight("cached", id, entity);
        Assertions.assertSame(entity, disabled.lookup("cached", id));
    }

    @Test
    public void testCyclicReferenceDoesNotStackOverflow() {
        NodeA a = new NodeA();
        a.name = "alpha";
        NodeB b = new NodeB();
        b.name = "beta";

        getDs().save(a);
        getDs().save(b);

        a.partner = b;
        b.partner = a;
        getDs().save(a);
        getDs().save(b);

        NodeA loaded = getDs().find(NodeA.class).filter(eq("_id", a.id)).first();
        Assertions.assertNotNull(loaded);
        Assertions.assertNotNull(loaded.partner);
        Assertions.assertEquals("beta", loaded.partner.name);
        Assertions.assertNotNull(loaded.partner.partner);
        Assertions.assertEquals("alpha", loaded.partner.partner.name);
    }

    /**
     * A projected document decodes to a partial entity, which must not be handed to a later reference as if it
     * were the whole entity.
     */
    @Test
    public void testProjectedEntitiesAreNotReusedForReferences() {
        Buddy b = new Buddy();
        b.name = "b";
        b.age = 2;
        getDs().save(b);
        Buddy a = new Buddy();
        a.name = "a";
        a.age = 1;
        a.buddy = b;
        getDs().save(a);

        // "b" decodes first, without its age, before "a" resolves its reference to it
        List<Buddy> loaded = getDs().find(Buddy.class, new FindOptions()
                .sort(Sort.descending("name"))
                .projection().include("name", "buddy"))
                .iterator()
                .toList();

        Assertions.assertEquals(2, loaded.size());
        Assertions.assertEquals(0, loaded.get(0).age, "the projection should have left out the age");
        Buddy referenced = loaded.get(1).buddy;
        Assertions.assertNotNull(referenced);
        Assertions.assertEquals(2, referenced.age, "the reference resolved to the projected, partial entity");
    }

    /**
     * DBRefs into different collections can share an id. A cached hit for one must not stand in for the other.
     */
    @Test
    public void testDbRefsSharingAnIdAcrossCollections() {
        Dog dog = new Dog();
        dog.id = "shared";
        dog.name = "dog";
        getDs().save(dog);
        Cat cat = new Cat();
        cat.id = "shared";
        cat.name = "cat";
        getDs().save(cat);

        Kennel kennel = new Kennel();
        kennel.id = new ObjectId();
        kennel.favorite = dog;
        kennel.animals = List.of(dog, cat);
        getDs().save(kennel);

        // favorite decodes first, so the dog is in the session by the time the list is resolved
        Kennel loaded = getDs().find(Kennel.class).first();
        Assertions.assertNotNull(loaded);
        Assertions.assertEquals(2, loaded.animals.size());
        Assertions.assertInstanceOf(Dog.class, loaded.animals.get(0));
        Assertions.assertEquals("dog", loaded.animals.get(0).name);
        Assertions.assertInstanceOf(Cat.class, loaded.animals.get(1));
        Assertions.assertEquals("cat", loaded.animals.get(1).name);
    }

    /**
     * An entity whose lifecycle callback fails part way through its decode must not be left in the session for
     * anything else to find.
     */
    @Test
    public void testFailedLifecycleCallbackIsNotLeftInSession() {
        Fussy fussy = new Fussy();
        getDs().save(fussy);
        String collection = getMapper().getEntityModel(Fussy.class).collectionName();

        for (String phase : List.of("pre", "post")) {
            DecodeSession session = new DecodeSession();
            Fussy.failIn = phase;
            try {
                session.decoding(() -> Assertions.assertThrows(RuntimeException.class,
                        () -> getDs().find(Fussy.class).first(), phase + "load should have failed"));
            } finally {
                Fussy.failIn = null;
            }
            Assertions.assertNull(session.lookup(collection, fussy.id),
                    "an entity whose @" + phase + "load failed was left in the session");
        }
    }

    /**
     * An entity built through a constructor taking its properties has no instance to publish until its decode
     * completes, so a cycle through it can not be resolved. It should fail clearly instead of overflowing the stack.
     */
    @Test
    public void testCyclicReferenceThroughConstructorFailsFast() {
        ObjectId a = new ObjectId();
        ObjectId b = new ObjectId();
        getMapper().map(LinkRecord.class);
        getDs().getDatabase().getCollection(getMapper().getEntityModel(LinkRecord.class).collectionName())
                .insertMany(List.of(
                        new Document("_id", a).append("name", "a").append("next", b),
                        new Document("_id", b).append("name", "b").append("next", a)));

        MappingException thrown = Assertions.assertThrows(MappingException.class,
                () -> getDs().find(LinkRecord.class).filter(eq("_id", a)).first());
        Assertions.assertTrue(thrown.getMessage().contains("Cyclic reference"), thrown.getMessage());
    }

    /**
     * A multi-dimensional reference nests its ids. Each one still has to be checked against the session, or a cycle
     * through a nested list recurses until the stack overflows.
     */
    @Test
    public void testCyclicReferenceInNestedArray() {
        Grid grid = new Grid();
        grid.name = "grid";
        getDs().save(grid);
        grid.cells = new Grid[][] { new Grid[] { grid } };
        getDs().save(grid);

        Grid loaded = getDs().find(Grid.class).first();
        Assertions.assertNotNull(loaded);
        Assertions.assertSame(loaded, loaded.cells[0][0]);
    }

    /**
     * {@code returnKey} returns only the index keys, so its documents decode to partial entities just as projected
     * ones do.
     */
    @Test
    public void testReturnKeyEntitiesAreNotReusedForReferences() {
        Buddy b = new Buddy();
        b.name = "b";
        b.age = 2;
        getDs().save(b);
        Buddy a = new Buddy();
        a.name = "a";
        a.age = 1;
        a.buddy = b;
        getDs().save(a);

        // an index covering _id and the reference, so the keys returned still carry both
        Document keys = new Document("buddy", 1).append("_id", 1);
        getDs().getCollection(Buddy.class).createIndex(keys);

        // "b" has no buddy, so it sorts first in the index and is decoded before "a" resolves its reference to it
        List<Buddy> loaded = getDs().find(Buddy.class, new FindOptions()
                .hint(keys)
                .returnKey(true))
                .iterator()
                .toList();

        Assertions.assertEquals(2, loaded.size());
        Assertions.assertNull(loaded.get(0).name, "returnKey should have left out the name");
        Buddy referenced = loaded.get(1).buddy;
        Assertions.assertNotNull(referenced);
        Assertions.assertEquals("b", referenced.name, "the reference resolved to the partial, returnKey entity");
    }

    /**
     * A projected find run part way through another decode -- from a lifecycle callback, say -- shares the outer
     * session. Its partial entities must not be cached there for later references to pick up.
     */
    @Test
    public void testNestedProjectedEntitiesAreNotReusedForReferences() {
        Buddy b = new Buddy();
        b.name = "b";
        b.age = 2;
        getDs().save(b);
        Buddy a = new Buddy();
        a.name = "a";
        a.age = 1;
        a.buddy = b;
        getDs().save(a);

        DecodeSession session = new DecodeSession();
        session.decoding(() -> {
            Buddy partial = getDs().find(Buddy.class, new FindOptions().projection().include("name"))
                    .filter(eq("name", "b"))
                    .first();
            Assertions.assertNotNull(partial);
            Assertions.assertEquals(0, partial.age, "the projection should have left out the age");

            Buddy loaded = getDs().find(Buddy.class).filter(eq("name", "a")).first();
            Assertions.assertNotNull(loaded);
            Assertions.assertNotNull(loaded.buddy);
            Assertions.assertEquals(2, loaded.buddy.age, "the reference resolved to the projected, partial entity");
            return null;
        });
    }

    /**
     * {@code knownFields} projects each property by its mapped name only, so values stored under an {@code @AlsoLoad}
     * name are left out and the entities it returns are partial.
     */
    @Test
    public void testKnownFieldsEntitiesAreNotReusedForReferences() {
        Veteran b = new Veteran();
        b.name = "b";
        b.age = 2;
        getDs().save(b);
        Veteran a = new Veteran();
        a.name = "a";
        a.age = 1;
        a.buddy = b;
        getDs().save(a);
        // move the age to its legacy name, which only @AlsoLoad knows about
        getDs().getCollection(Veteran.class).withDocumentClass(Document.class)
                .updateMany(new Document(), new Document("$rename", new Document("age", "years")));

        // "b" decodes first, without its age, before "a" resolves its reference to it
        List<Veteran> loaded = getDs().find(Veteran.class, new FindOptions()
                .sort(Sort.descending("name"))
                .projection().knownFields())
                .iterator()
                .toList();

        Assertions.assertEquals(2, loaded.size());
        Assertions.assertEquals(0, loaded.get(0).age, "knownFields should have left out the @AlsoLoad name");
        Veteran referenced = loaded.get(1).buddy;
        Assertions.assertNotNull(referenced);
        Assertions.assertEquals(2, referenced.age, "the reference resolved to the knownFields, partial entity");
    }

    /**
     * MongoDB tells embedded-document ids apart by field order, so {@code {a: 1, b: 2}} and {@code {b: 2, a: 1}} are two
     * documents. The session must not treat a reference to one as a hit for the other.
     */
    @Test
    public void testDocumentIdsDifferingOnlyInFieldOrder() {
        DocIdTarget first = new DocIdTarget();
        first.id = new Document("a", 1).append("b", 2);
        first.name = "first";
        getDs().save(first);
        DocIdTarget second = new DocIdTarget();
        second.id = new Document("b", 2).append("a", 1);
        second.name = "second";
        getDs().save(second);

        DocIdHolder holder = new DocIdHolder();
        holder.id = new ObjectId();
        holder.first = first;
        holder.second = second;
        holder.both = List.of(first, second);
        getDs().save(holder);

        DocIdHolder loaded = getDs().find(DocIdHolder.class).first();
        Assertions.assertNotNull(loaded);
        Assertions.assertEquals("first", loaded.first.name);
        Assertions.assertEquals("second", loaded.second.name, "the reference resolved to the entity with the reordered id");
        Assertions.assertEquals(List.of("first", "second"), loaded.both.stream().map(t -> t.name).toList());
    }

    /**
     * References look entities up by their id as stored. An entity built through its constructor must be cached under
     * that same value, not its converted Java form, or every reference to it fetches it again.
     */
    @Test
    public void testConstructorEntityWithConvertedIdIsDeduplicated() {
        CharRecord target = new CharRecord('x', "target");
        getDs().save(target);
        CharHolder holder = new CharHolder();
        holder.id = new ObjectId();
        holder.first = target;
        holder.second = target;
        getDs().save(holder);

        CharHolder loaded = getDs().find(CharHolder.class).first();
        Assertions.assertNotNull(loaded);
        Assertions.assertNotNull(loaded.first);
        Assertions.assertSame(loaded.first, loaded.second);
    }

    @Entity("grids")
    private static class Grid {
        @Id
        private ObjectId id = new ObjectId();
        private String name;
        @Reference
        private Grid[][] cells;
    }

    @Entity("char_records")
    record CharRecord(@Id Character id, String name) {
    }

    @Entity("char_holders")
    private static class CharHolder {
        @Id
        private ObjectId id;
        @Reference(idOnly = true)
        private CharRecord first;
        @Reference(idOnly = true)
        private CharRecord second;
    }

    @Entity("buddies")
    private static class Buddy {
        @Id
        private ObjectId id = new ObjectId();
        private String name;
        private int age;
        @Reference
        private Buddy buddy;
    }

    @Entity("veterans")
    private static class Veteran {
        @Id
        private ObjectId id = new ObjectId();
        private String name;
        @AlsoLoad("years")
        private int age;
        @Reference(idOnly = true)
        private Veteran buddy;
    }

    @Entity("docIdTargets")
    private static class DocIdTarget {
        @Id
        private Document id;
        private String name;
    }

    @Entity("docIdHolders")
    private static class DocIdHolder {
        @Id
        private ObjectId id;
        @Reference(idOnly = true)
        private DocIdTarget first;
        @Reference(idOnly = true)
        private DocIdTarget second;
        @Reference(idOnly = true)
        private List<DocIdTarget> both;
    }

    @Entity("animals")
    private static class Animal {
        @Id
        protected String id;
        protected String name;
    }

    @Entity("dogs")
    private static class Dog extends Animal {
    }

    @Entity("cats")
    private static class Cat extends Animal {
    }

    @Entity("kennels")
    private static class Kennel {
        @Id
        private ObjectId id;
        @Reference
        private Animal favorite;
        @Reference
        private List<Animal> animals;
    }

    @Entity("fussy")
    private static class Fussy {
        static String failIn;

        @Id
        private ObjectId id = new ObjectId();

        @PreLoad
        void preLoad() {
            if ("pre".equals(failIn)) {
                throw new IllegalStateException("failing @PreLoad on purpose");
            }
        }

        @PostLoad
        void postLoad() {
            if ("post".equals(failIn)) {
                throw new IllegalStateException("failing @PostLoad on purpose");
            }
        }
    }

    @Entity("link_records")
    record LinkRecord(@Id ObjectId id, String name, @Reference(idOnly = true) LinkRecord next) {
    }
}
