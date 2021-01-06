package dev.morphia.test.mapping.experimental;

import com.mongodb.DBRef;
import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.stages.Lookup;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.Author;
import dev.morphia.test.models.Book;
import dev.morphia.test.models.FacebookUser;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.morphia.Morphia.createDatastore;
import static dev.morphia.aggregation.experimental.stages.Unwind.on;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class TestReferences extends TestBase {
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

        System.out.println(getDs().getMapper().getCollection(Author.class).withDocumentClass(BsonDocument.class).find().first().toJson());
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println(getDs().getMapper().getCollection(Book.class).withDocumentClass(BsonDocument.class).find().first().toJson());

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
        Assert.assertTrue(foundBook.author.isResolved());
        Assert.assertEquals(author, foundBook.author.get());
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
}
