package dev.morphia.test;

import java.lang.annotation.Annotation;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.lang.NonNull;

import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.EntityListener;
import dev.morphia.InsertManyOptions;
import dev.morphia.InsertOneOptions;
import dev.morphia.MissingIdException;
import dev.morphia.ModifyOptions;
import dev.morphia.MorphiaDatastore;
import dev.morphia.UpdateOptions;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.EntityListeners;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Transient;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.CountOptions;
import dev.morphia.query.FindAndDeleteOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.QueryException;
import dev.morphia.query.ValidationException;
import dev.morphia.test.datastore.MultipleDSEntity;
import dev.morphia.test.models.Address;
import dev.morphia.test.models.Book;
import dev.morphia.test.models.City;
import dev.morphia.test.models.CurrentStatus;
import dev.morphia.test.models.FacebookUser;
import dev.morphia.test.models.Grade;
import dev.morphia.test.models.Hotel;
import dev.morphia.test.models.Population;
import dev.morphia.test.models.Rectangle;
import dev.morphia.test.models.TestEntity;
import dev.morphia.test.models.User;

import org.bson.Document;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import static com.mongodb.client.model.CollationStrength.SECONDARY;
import static com.mongodb.client.model.ReturnDocument.AFTER;
import static com.mongodb.client.model.ReturnDocument.BEFORE;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.inc;
import static dev.morphia.query.updates.UpdateOperators.set;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.List.of;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

@SuppressWarnings({ "rawtypes", "ConstantConditions" })
public class TestDatastore extends TestBase {

    public TestDatastore() {
        super(buildConfig()
                .applyCaps(true)
                .packages(of(LifecycleTestObj.class.getPackageName(),
                        FacebookUser.class.getPackageName())));
    }

    @Test
    public void testDatastoreClones() {
        withConfig(buildConfig(MultipleDSEntity.class), () -> {
            assertEquals(getMapper().getMappedEntities().size(), 1);

            MorphiaDatastore copied = new MorphiaDatastore(getDs());

            EntityModel model = getMapper().getEntityModel(MultipleDSEntity.class);
            EntityModel copiedModel = copied.getMapper().getEntityModel(MultipleDSEntity.class);

            assertNotSame(model, copiedModel);
            assertNotSame(model.getProperty("_id"), copiedModel.getProperty("_id"));
            assertNotSame(model.getProperty("name"), copiedModel.getProperty("name"));
            assertNotSame(model.getProperty("count"), copiedModel.getProperty("count"));
        });
    }

    @Test
    public void testAlternateCollections() {
        final String alternateName = "alternate";

        Book book = getDs().save(new Book(), new InsertOneOptions()
                .collection(alternateName));

        Book first = getDs().find(Book.class,
                new FindOptions().collection(alternateName))
                .filter(eq("_id", book.id))
                .first();
        assertEquals(first, book);

        getDs().find(Book.class)
                .delete(new DeleteOptions()
                        .collection(alternateName));
        long count = getDs()
                .find(Book.class)
                .filter(eq("_id", book.id))
                .count(new CountOptions().collection(alternateName));
        assertEquals(count, 0);

        book = new Book();
        User user = new User();
        getDs().save(of(book, user), new InsertManyOptions()
                .collection(alternateName));
        List<Document> list = getDatabase().getCollection(alternateName)
                .find(Filters.in("_id", book.id, user.getId()))
                .projection(new Document("_id", 1))
                .into(new ArrayList<>());

        assertEquals(list.stream()
                .map(d -> d.getObjectId("_id"))
                .collect(Collectors.toList()),
                of(book.id, user.getId()));

        getDs().find(Book.class)
                .delete(new DeleteOptions()
                        .collection(alternateName)
                        .multi(true));

        getDs().save(new Book(), new InsertOneOptions()
                .collection(alternateName));

        Book modify = getDs().find(Book.class)
                .modify(new ModifyOptions()
                        .collection(alternateName)
                        .returnDocument(AFTER),
                        inc("copies", 10));

        assertEquals(modify.copies, 10);

        getDs().find(Book.class, new FindOptions().collection(alternateName))
                .update(new UpdateOptions()
                        .collection(alternateName),
                        set("copies", 42));

        book = getDs().find(Book.class, new FindOptions().collection(alternateName)).first();

        assertEquals(book.copies, 42);

        Book delete = getDs().find(Book.class)
                .filter(eq("_id", book.id))
                .findAndDelete(new FindAndDeleteOptions()
                        .collection(alternateName));

        assertEquals(delete, book);
    }

    @Test
    public void testBulkInsert() {
        MongoCollection testEntity = getDs().getCollection(TestEntity.class);
        MongoCollection population = getDs().getCollection(Population.class);
        this.getDs().insert(asList(new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()),
                new InsertManyOptions().writeConcern(WriteConcern.ACKNOWLEDGED));
        assertEquals(testEntity.countDocuments(), 5);

        testEntity.drop();
        population.drop();
        this.getDs().insert(asList(new TestEntity(), new TestEntity(), new Population(), new Population(), new Population()),
                new InsertManyOptions()
                        .writeConcern(WriteConcern.ACKNOWLEDGED));
        assertEquals(testEntity.countDocuments(), 2);
        assertEquals(population.countDocuments(), 3);
    }

    @Test
    public void testCappedEntity() {
        assertCapped(CurrentStatus.class, 1);

        // when-then
        Query<CurrentStatus> query = getDs().find(CurrentStatus.class);

        getDs().save(new CurrentStatus("All Good"));
        assertEquals(query.count(), 1);

        getDs().save(new CurrentStatus("Kinda Bad"));
        assertEquals(query.count(), 1);

        assertTrue(query.iterator()
                .next().message.contains("Bad"));

        getDs().save(new CurrentStatus("Kinda Bad2"));
        assertEquals(query.count(), 1);

        getDs().save(new CurrentStatus("Kinda Bad3"));
        assertEquals(query.count(), 1);

        getDs().save(new CurrentStatus("Kinda Bad4"));
        assertEquals(query.count(), 1);
    }

    @Test
    public void testCollectionNames() {
        assertEquals(getMapper().getEntityModel(FacebookUser.class).collectionName(), "facebook_users");
    }

    @Test
    public void testCustomCodecProvider() {
        getDs().save(new User("Christopher Turk", LocalDate.of(1974, Month.JUNE, 22)));
        withConfig(buildConfig()
                .codecProvider(new AlwaysFailingCodecProvider()), () -> {
                    assertThrows(QueryException.class,
                            () -> getDs().save(new User("John \"J.D.\" Dorian", LocalDate.of(1974, Month.APRIL, 6))));
                    assertThrows(QueryException.class, () -> getDs().find(User.class).first());

                    assertThrows(QueryException.class, () -> getDs().getCodecRegistry()
                            .get(String.class)
                            .encode(new DocumentWriter(getMapper().getConfig()), "this should fail", EncoderContext.builder().build()));
                });
    }

    @Test
    public void testDeleteWithCollation() {
        getDs().save(asList(new FacebookUser(1, "John Doe"),
                new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                .filter(eq("username", "john doe"));
        assertEquals(query.delete().getDeletedCount(), 1);

        assertEquals(query.delete(new DeleteOptions()
                .collation(Collation.builder()
                        .locale("en")
                        .collationStrength(SECONDARY)
                        .build()))
                .getDeletedCount(), 1);
    }

    @Test
    public void testDeletes() {
        for (int i = 0; i < 100; i++) {
            getDs().save(new City());
        }
        DeleteResult delete = getDs().find(City.class).delete();
        assertEquals(delete.getDeletedCount(), 1, "Should only delete 1");

        City first = getDs().find(City.class).first();
        delete = getDs().delete(first);
        assertEquals(delete.getDeletedCount(), 1, "Should only delete 1");

        first = getDs().find(City.class).first();
        delete = getDs().delete(first, new DeleteOptions().multi(true));
        assertEquals(delete.getDeletedCount(), 1, "Should only delete 1");

        delete = getDs().find(City.class).delete(new DeleteOptions().multi(true));
        assertTrue(delete.getDeletedCount() > 1, "Should the rest");
    }

    @Test
    public void testDoesNotExistAfterDelete() {
        // given
        long id = System.currentTimeMillis();
        final long key = getDs().save(new FacebookUser(id, "user 1")).getId();

        // when
        getDs().find(FacebookUser.class).findAndDelete();

        // then
        assertNull(getDs().find(FacebookUser.class)
                .filter(eq("_id", key))
                .first(), "Shouldn't exist after delete");
    }

    @Test
    public void testEmbedded() {
        getDs().find(Hotel.class).findAndDelete();
        final Hotel borg = new Hotel();
        borg.setName("Hotel Borg");
        borg.setStars(4);
        borg.setTakesCreditCards(true);
        borg.setStartDate(new Date());
        borg.setType(Hotel.Type.LEISURE);
        final Address address = new Address();
        address.setStreet("Posthusstraeti 11");
        address.setPostCode("101");
        borg.setAddress(address);

        getDs().save(borg);
        assertEquals(getDs().find(Hotel.class).count(), 1);
        assertNotNull(borg.getId());

        final Hotel hotelLoaded = getDs().find(Hotel.class)
                .filter(eq("_id", borg.getId()))
                .first();
        assertEquals(borg.getName(), hotelLoaded.getName());
        assertEquals(borg.getAddress().getPostCode(), hotelLoaded.getAddress().getPostCode());
    }

    @Test
    public void testFindAndDeleteWithCollation() {
        getDs().save(asList(new FacebookUser(1, "John Doe"),
                new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                .filter(eq("username", "john doe"));
        assertNotNull(query.findAndDelete());
        assertNull(query.findAndDelete());

        FindAndDeleteOptions options = new FindAndDeleteOptions()
                .collation(Collation.builder()
                        .locale("en")
                        .collationStrength(SECONDARY)
                        .build());
        assertNotNull(query.findAndDelete(options));
        assertNull(query.iterator().tryNext());
    }

    @Test
    public void testFindAndDeleteWithNoQueryMatch() {
        assertNull(getDs().find(FacebookUser.class)
                .filter(eq("username", "David S. Pumpkins"))
                .findAndDelete());
    }

    @Test
    public void testFindAndModify() {
        getDs().save(asList(new FacebookUser(1, "John Doe"),
                new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                .filter(eq("username", "john doe"));

        FacebookUser modified = query.modify(inc("loginCount"));
        assertEquals(modified.loginCount, 0);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 1)).first().loginCount, 0);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 2)).first().loginCount, 1);

        modified = query.modify(new ModifyOptions().returnDocument(AFTER), inc("loginCount"));
        assertEquals(modified.loginCount, 2);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 1)).first().loginCount, 0);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 2)).first().loginCount, 2);

        assertNull(getDs().find(FacebookUser.class)
                .filter(eq("id", 3L),
                        eq("username", "Jon Snow"))
                .modify(new ModifyOptions()
                        .returnDocument(BEFORE)
                        .upsert(true),
                        inc("loginCount", 4)));

        FacebookUser user = getDs().find(FacebookUser.class).filter(eq("id", 3)).first();
        assertEquals(user.loginCount, 4);
        assertEquals(user.username, "Jon Snow");

        FacebookUser results = getDs().find(FacebookUser.class)
                .filter(eq("id", 4L),
                        eq("username", "Ron Swanson"))
                .modify(new ModifyOptions()
                        .returnDocument(AFTER)
                        .upsert(true),
                        inc("loginCount"));
        assertEquals(results.loginCount, 1);
        assertEquals(results.username, "Ron Swanson");

        user = getDs().find(FacebookUser.class).filter(eq("id", 4)).iterator().next();
        assertEquals(user.loginCount, 1);
        assertEquals(user.username, "Ron Swanson");
    }

    @Test
    public void testFindAndModifyWithOptions() {
        getDs().save(asList(new FacebookUser(1, "John Doe"),
                new FacebookUser(2, "john doe")));

        FacebookUser result = getDs().find(FacebookUser.class)
                .filter(eq("username", "john doe"))
                .modify(inc("loginCount"));

        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator()
                .next().loginCount, 0);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator()
                .next().loginCount, 1);
        assertEquals(result.loginCount, 0);

        result = getDs().find(FacebookUser.class)
                .filter(eq("username", "john doe"))
                .modify(new ModifyOptions()
                        .returnDocument(BEFORE)
                        .collation(Collation.builder()
                                .locale("en")
                                .collationStrength(SECONDARY)
                                .build()),
                        inc("loginCount"));
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator()
                .next().loginCount, 1);
        assertEquals(result.loginCount, 0);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator()
                .next().loginCount, 1);

        result = getDs().find(FacebookUser.class)
                .filter(eq("id", 3L),
                        eq("username", "Jon Snow"))
                .modify(new ModifyOptions()
                        .returnDocument(BEFORE)
                        .upsert(true),
                        inc("loginCount"));

        assertNull(result);
        FacebookUser user = getDs().find(FacebookUser.class).filter(eq("id", 3)).iterator()
                .next();
        assertEquals(user.loginCount, 1);
        assertEquals(user.username, "Jon Snow");

        result = getDs().find(FacebookUser.class)
                .filter(eq("id", 4L),
                        eq("username", "Ron Swanson"))
                .modify(new ModifyOptions()
                        .returnDocument(AFTER)
                        .upsert(true),
                        inc("loginCount"));

        assertNotNull(result);
        user = getDs().find(FacebookUser.class).filter(eq("id", 4)).iterator()
                .next();
        assertEquals(result.loginCount, 1);
        assertEquals(result.username, "Ron Swanson");
        assertEquals(user.loginCount, 1);
        assertEquals(user.username, "Ron Swanson");
    }

    @Test
    public void testIdUpdatedOnSave() {
        final Rectangle rect = new Rectangle(10, 10);
        getDs().save(rect);
        assertNotNull(rect.getId());
    }

    @Test
    public void testInsert() {
        MongoCollection collection = getDs().getCollection(TestEntity.class);
        this.getDs().insert(new TestEntity());
        assertEquals(collection.countDocuments(), 1);
        this.getDs().insert(new TestEntity(), new InsertOneOptions()
                .writeConcern(WriteConcern.ACKNOWLEDGED));
        assertEquals(collection.countDocuments(), 2);
    }

    @Test
    public void testReplace() {
        User bob = new User("bob", LocalDate.now());
        User linda = new User("linda", LocalDate.now());

        assertThrows(MissingIdException.class, () -> this.getDs().replace(bob));

        assertThrows(MissingIdException.class, () -> this.getDs().replace(List.of(bob, linda)));

        this.getDs().insert(bob);
        assertEquals(getDs().find(User.class).count(), 1);
        this.getDs().insert(linda);

        bob.setLikes(List.of("burgers"));
        getDs().replace(bob);
        assertEquals(getDs().find(User.class).first().getLikes(), List.of("burgers"));

        bob.setLikes(List.of("burgers", "linda"));
        linda.setLikes(List.of("bob", "tina", "gene", "louise"));
        getDs().replace(List.of(bob, linda));

        for (User user : getDs().find(User.class)) {
            if (user.name.equals("bob")) {
                assertEquals(user.getLikes(), List.of("burgers", "linda"));
            } else {
                assertEquals(user.getLikes(), List.of("bob", "tina", "gene", "louise"));
            }
        }
    }

    @Test
    public void testInsertEmpty() {
        this.getDs().insert(emptyList());
        this.getDs().insert(emptyList(), new InsertManyOptions()
                .writeConcern(WriteConcern.ACKNOWLEDGED));
    }

    @Test
    public void testLifecycle() {
        final LifecycleTestObj life1 = new LifecycleTestObj();
        getDs().save(life1);
        assertTrue(LifecycleListener.foundDatastore);
        assertTrue(life1.prePersist);
        assertTrue(life1.prePersistWithParam);
        assertTrue(life1.prePersistWithParamAndReturn);
        assertTrue(life1.postPersist);
        assertTrue(life1.postPersistWithParam);

        final Datastore datastore = getDs();

        final LifecycleTestObj loaded = datastore.find(LifecycleTestObj.class)
                .filter(eq("_id", life1.id))
                .first();
        assertTrue(loaded.preLoad);
        assertTrue(loaded.preLoadWithParam);
        assertTrue(loaded.postLoad);
        assertTrue(loaded.postLoadWithParam);
    }

    @Test
    public void testLifecycleListeners() {
        final LifecycleTestObj life1 = new LifecycleTestObj();
        getDs().save(life1);
        assertTrue(LifecycleListener.prePersist);
        assertTrue(LifecycleListener.prePersistWithEntity);
    }

    @Test
    public void testModifyWithBadPaths() {
        Query<LifecycleTestObj> query = getDs().find(LifecycleTestObj.class);

        assertThrows(ValidationException.class, () -> query.modify(inc("some.field", 2)));

        query.first();
    }

    @Test
    public void testRefresh() {
        FacebookUser steve = getDs().save(new FacebookUser(1, "Steve"));

        assertEquals(steve.loginCount, 0);
        UpdateResult loginCount = getDs().find(FacebookUser.class)
                .update(inc("loginCount", 10));

        assertEquals(loginCount.getModifiedCount(), 1);

        getDs().refresh(steve);
        assertEquals(steve.loginCount, 10);

        loginCount = getDs().find(FacebookUser.class)
                .update(
                        set("username", "Mark"),
                        set("loginCount", 1));

        assertEquals(loginCount.getModifiedCount(), 1);
        getDs().refresh(steve);
        assertEquals(steve.loginCount, 1);
        assertEquals(steve.username, "Mark");

    }

    @Test
    public void testSaveWithNoID() {
        Grade grade = new Grade();
        grade.marks = 80;

        assertThrows(MappingException.class, () -> getDs().save(grade));

        assertThrows(MappingException.class, () -> getDs().save(of(grade, grade)));

    }

    @Test
    public void testUpdateWithCollation() {
        getDs().save(asList(new FacebookUser(1, "John Doe"),
                new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                .filter(eq("username", "john doe"));

        UpdateResult results = query.update(inc("loginCount"));

        assertEquals(results.getModifiedCount(), 1);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator().next().loginCount, 0);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator()
                .next().loginCount, 1);

        results = query.update(new UpdateOptions()
                .multi(true)
                .collation(Collation.builder()
                        .locale("en")
                        .collationStrength(SECONDARY)
                        .build()),
                inc("loginCount"));
        assertEquals(results.getModifiedCount(), 2);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator()
                .next().loginCount, 1);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator()
                .next().loginCount, 2);
    }

    private static class LifecycleListener implements EntityListener<LifecycleTestObj> {
        private static boolean prePersist;
        private static boolean prePersistWithEntity;
        private static boolean foundDatastore;

        @Override
        public boolean hasAnnotation(@NonNull Class<? extends Annotation> type) {
            return type.equals(PrePersist.class);
        }

        @PrePersist
        void prePersist(Datastore datastore) {
            foundDatastore = datastore != null;
            prePersist = true;
        }

        @PrePersist
        void prePersist(LifecycleTestObj obj) {
            if (obj == null) {
                throw new RuntimeException();
            }
            prePersistWithEntity = true;

        }
    }

    @Entity
    @SuppressWarnings("UnusedDeclaration")
    @EntityListeners(LifecycleListener.class)
    private static class LifecycleTestObj {
        @Id
        private ObjectId id;
        @Transient
        private boolean prePersist;
        @Transient
        private boolean postPersist;
        @Transient
        private boolean preLoad;
        @Transient
        private boolean postLoad;
        @Transient
        private boolean postLoadWithParam;
        private boolean prePersistWithParamAndReturn;
        private boolean prePersistWithParam;
        private boolean postPersistWithParam;
        private boolean preLoadWithParamAndReturn;
        private boolean preLoadWithParam;

        @PrePersist
        public Document prePersistWithParamAndReturn(Document document) {
            if (prePersistWithParamAndReturn) {
                throw new RuntimeException("already called");
            }
            prePersistWithParamAndReturn = true;
            return null;
        }

        @PrePersist
        protected void prePersistWithParam(Document document) {
            if (prePersistWithParam) {
                throw new RuntimeException("already called");
            }
            prePersistWithParam = true;
        }

        @PostPersist
        private void postPersistPersist() {
            if (postPersist) {
                throw new RuntimeException("already called");
            }
            postPersist = true;

        }

        @PostLoad
        void postLoad() {
            if (postLoad) {
                throw new RuntimeException("already called");
            }

            postLoad = true;
        }

        @PostLoad
        void postLoadWithParam(Document document) {
            if (postLoadWithParam) {
                throw new RuntimeException("already called");
            }
            postLoadWithParam = true;
        }

        @PostPersist
        void postPersistWithParam(Document document) {
            postPersistWithParam = true;
            if (!document.containsKey("_id")) {
                throw new RuntimeException("missing " + "_id");
            }
        }

        @PreLoad
        void preLoad() {
            if (preLoad) {
                throw new RuntimeException("already called");
            }

            preLoad = true;
        }

        @PreLoad
        void preLoadWithParam(Document document) {
            document.put("preLoadWithParam", true);
        }

        @PreLoad
        Document preLoadWithParamAndReturn(Document document) {
            final Document retObj = new Document();
            retObj.putAll(document);
            retObj.put("preLoadWithParamAndReturn", true);
            return retObj;
        }

        @PrePersist
        void prePersist() {
            if (prePersist) {
                throw new RuntimeException("already called");
            }

            prePersist = true;
        }
    }

}
