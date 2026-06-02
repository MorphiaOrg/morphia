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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.mongodb.client.model.CollationStrength.SECONDARY;
import static com.mongodb.client.model.ReturnDocument.AFTER;
import static com.mongodb.client.model.ReturnDocument.BEFORE;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.inc;
import static dev.morphia.query.updates.UpdateOperators.set;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.List.of;

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
            Assertions.assertEquals(1, getMapper().getMappedEntities().size());

            MorphiaDatastore copied = new MorphiaDatastore(getDs());

            EntityModel model = getMapper().getEntityModel(MultipleDSEntity.class);
            EntityModel copiedModel = copied.getMapper().getEntityModel(MultipleDSEntity.class);

            Assertions.assertNotSame(copiedModel, model);
            Assertions.assertNotSame(copiedModel.getProperty("_id"), model.getProperty("_id"));
            Assertions.assertNotSame(copiedModel.getProperty("name"), model.getProperty("name"));
            Assertions.assertNotSame(copiedModel.getProperty("count"), model.getProperty("count"));
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
        Assertions.assertEquals(book, first);

        getDs().find(Book.class)
                .delete(new DeleteOptions()
                        .collection(alternateName));
        long count = getDs()
                .find(Book.class)
                .filter(eq("_id", book.id))
                .count(new CountOptions().collection(alternateName));
        Assertions.assertEquals(0, count);

        book = new Book();
        User user = new User();
        getDs().save(of(book, user), new InsertManyOptions()
                .collection(alternateName));
        List<Document> list = getDatabase().getCollection(alternateName)
                .find(Filters.in("_id", book.id, user.getId()))
                .projection(new Document("_id", 1))
                .into(new ArrayList<>());

        Assertions.assertEquals(of(book.id, user.getId()), list.stream()
                .map(d -> d.getObjectId("_id"))
                .collect(Collectors.toList()));

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

        Assertions.assertEquals(10, modify.copies);

        getDs().find(Book.class, new FindOptions().collection(alternateName))
                .update(new UpdateOptions()
                        .collection(alternateName),
                        set("copies", 42));

        book = getDs().find(Book.class, new FindOptions().collection(alternateName)).first();

        Assertions.assertEquals(42, book.copies);

        Book delete = getDs().find(Book.class)
                .filter(eq("_id", book.id))
                .findAndDelete(new FindAndDeleteOptions()
                        .collection(alternateName));

        Assertions.assertEquals(book, delete);
    }

    @Test
    public void testBulkInsert() {
        MongoCollection testEntity = getDs().getCollection(TestEntity.class);
        MongoCollection population = getDs().getCollection(Population.class);
        this.getDs().insert(asList(new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()),
                new InsertManyOptions().writeConcern(WriteConcern.ACKNOWLEDGED));
        Assertions.assertEquals(5, testEntity.countDocuments());

        testEntity.drop();
        population.drop();
        this.getDs().insert(asList(new TestEntity(), new TestEntity(), new Population(), new Population(), new Population()),
                new InsertManyOptions()
                        .writeConcern(WriteConcern.ACKNOWLEDGED));
        Assertions.assertEquals(2, testEntity.countDocuments());
        Assertions.assertEquals(3, population.countDocuments());
    }

    @Test
    public void testCappedEntity() {
        assertCapped(CurrentStatus.class, 1);

        // when-then
        Query<CurrentStatus> query = getDs().find(CurrentStatus.class);

        getDs().save(new CurrentStatus("All Good"));
        Assertions.assertEquals(1, query.count());

        getDs().save(new CurrentStatus("Kinda Bad"));
        Assertions.assertEquals(1, query.count());

        Assertions.assertTrue(query.iterator()
                .next().message.contains("Bad"));

        getDs().save(new CurrentStatus("Kinda Bad2"));
        Assertions.assertEquals(1, query.count());

        getDs().save(new CurrentStatus("Kinda Bad3"));
        Assertions.assertEquals(1, query.count());

        getDs().save(new CurrentStatus("Kinda Bad4"));
        Assertions.assertEquals(1, query.count());
    }

    @Test
    public void testCollectionNames() {
        Assertions.assertEquals("facebook_users", getMapper().getEntityModel(FacebookUser.class).collectionName());
    }

    @Test
    public void testCustomCodecProvider() {
        getDs().save(new User("Christopher Turk", LocalDate.of(1974, Month.JUNE, 22)));
        withConfig(buildConfig()
                .codecProvider(new AlwaysFailingCodecProvider()), () -> {
                    Assertions.assertThrows(QueryException.class,
                            () -> getDs().save(new User("John \"J.D.\" Dorian", LocalDate.of(1974, Month.APRIL, 6))));
                    Assertions.assertThrows(QueryException.class, () -> getDs().find(User.class).first());

                    Assertions.assertThrows(QueryException.class, () -> getDs().getCodecRegistry()
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
        Assertions.assertEquals(1, query.delete().getDeletedCount());

        Assertions.assertEquals(1, query.delete(new DeleteOptions()
                .collation(Collation.builder()
                        .locale("en")
                        .collationStrength(SECONDARY)
                        .build()))
                .getDeletedCount());
    }

    @Test
    public void testDeletes() {
        for (int i = 0; i < 100; i++) {
            getDs().save(new City());
        }
        DeleteResult delete = getDs().find(City.class).delete();
        Assertions.assertEquals(1, delete.getDeletedCount(), "Should only delete 1");

        City first = getDs().find(City.class).first();
        delete = getDs().delete(first);
        Assertions.assertEquals(1, delete.getDeletedCount(), "Should only delete 1");

        first = getDs().find(City.class).first();
        delete = getDs().delete(first, new DeleteOptions().multi(true));
        Assertions.assertEquals(1, delete.getDeletedCount(), "Should only delete 1");

        delete = getDs().find(City.class).delete(new DeleteOptions().multi(true));
        Assertions.assertTrue(delete.getDeletedCount() > 1, "Should the rest");
    }

    @Test
    public void testDoesNotExistAfterDelete() {
        // given
        long id = System.currentTimeMillis();
        final long key = getDs().save(new FacebookUser(id, "user 1")).getId();

        // when
        getDs().find(FacebookUser.class).findAndDelete();

        // then
        Assertions.assertNull(getDs().find(FacebookUser.class)
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
        Assertions.assertEquals(1, getDs().find(Hotel.class).count());
        Assertions.assertNotNull(borg.getId());

        final Hotel hotelLoaded = getDs().find(Hotel.class)
                .filter(eq("_id", borg.getId()))
                .first();
        Assertions.assertEquals(hotelLoaded.getName(), borg.getName());
        Assertions.assertEquals(hotelLoaded.getAddress().getPostCode(), borg.getAddress().getPostCode());
    }

    @Test
    public void testFindAndDeleteWithCollation() {
        getDs().save(asList(new FacebookUser(1, "John Doe"),
                new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                .filter(eq("username", "john doe"));
        Assertions.assertNotNull(query.findAndDelete());
        Assertions.assertNull(query.findAndDelete());

        FindAndDeleteOptions options = new FindAndDeleteOptions()
                .collation(Collation.builder()
                        .locale("en")
                        .collationStrength(SECONDARY)
                        .build());
        Assertions.assertNotNull(query.findAndDelete(options));
        Assertions.assertNull(query.iterator().tryNext());
    }

    @Test
    public void testFindAndDeleteWithNoQueryMatch() {
        Assertions.assertNull(getDs().find(FacebookUser.class)
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
        Assertions.assertEquals(0, modified.loginCount);
        Assertions.assertEquals(0, getDs().find(FacebookUser.class).filter(eq("id", 1)).first().loginCount);
        Assertions.assertEquals(1, getDs().find(FacebookUser.class).filter(eq("id", 2)).first().loginCount);

        modified = query.modify(new ModifyOptions().returnDocument(AFTER), inc("loginCount"));
        Assertions.assertEquals(2, modified.loginCount);
        Assertions.assertEquals(0, getDs().find(FacebookUser.class).filter(eq("id", 1)).first().loginCount);
        Assertions.assertEquals(2, getDs().find(FacebookUser.class).filter(eq("id", 2)).first().loginCount);

        Assertions.assertNull(getDs().find(FacebookUser.class)
                .filter(eq("id", 3L),
                        eq("username", "Jon Snow"))
                .modify(new ModifyOptions()
                        .returnDocument(BEFORE)
                        .upsert(true),
                        inc("loginCount", 4)));

        FacebookUser user = getDs().find(FacebookUser.class).filter(eq("id", 3)).first();
        Assertions.assertEquals(4, user.loginCount);
        Assertions.assertEquals("Jon Snow", user.username);

        FacebookUser results = getDs().find(FacebookUser.class)
                .filter(eq("id", 4L),
                        eq("username", "Ron Swanson"))
                .modify(new ModifyOptions()
                        .returnDocument(AFTER)
                        .upsert(true),
                        inc("loginCount"));
        Assertions.assertEquals(1, results.loginCount);
        Assertions.assertEquals("Ron Swanson", results.username);

        user = getDs().find(FacebookUser.class).filter(eq("id", 4)).iterator().next();
        Assertions.assertEquals(1, user.loginCount);
        Assertions.assertEquals("Ron Swanson", user.username);
    }

    @Test
    public void testFindAndModifyWithOptions() {
        getDs().save(asList(new FacebookUser(1, "John Doe"),
                new FacebookUser(2, "john doe")));

        FacebookUser result = getDs().find(FacebookUser.class)
                .filter(eq("username", "john doe"))
                .modify(inc("loginCount"));

        Assertions.assertEquals(0, getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator()
                .next().loginCount);
        Assertions.assertEquals(1, getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator()
                .next().loginCount);
        Assertions.assertEquals(0, result.loginCount);

        result = getDs().find(FacebookUser.class)
                .filter(eq("username", "john doe"))
                .modify(new ModifyOptions()
                        .returnDocument(BEFORE)
                        .collation(Collation.builder()
                                .locale("en")
                                .collationStrength(SECONDARY)
                                .build()),
                        inc("loginCount"));
        Assertions.assertEquals(1, getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator()
                .next().loginCount);
        Assertions.assertEquals(0, result.loginCount);
        Assertions.assertEquals(1, getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator()
                .next().loginCount);

        result = getDs().find(FacebookUser.class)
                .filter(eq("id", 3L),
                        eq("username", "Jon Snow"))
                .modify(new ModifyOptions()
                        .returnDocument(BEFORE)
                        .upsert(true),
                        inc("loginCount"));

        Assertions.assertNull(result);
        FacebookUser user = getDs().find(FacebookUser.class).filter(eq("id", 3)).iterator()
                .next();
        Assertions.assertEquals(1, user.loginCount);
        Assertions.assertEquals("Jon Snow", user.username);

        result = getDs().find(FacebookUser.class)
                .filter(eq("id", 4L),
                        eq("username", "Ron Swanson"))
                .modify(new ModifyOptions()
                        .returnDocument(AFTER)
                        .upsert(true),
                        inc("loginCount"));

        Assertions.assertNotNull(result);
        user = getDs().find(FacebookUser.class).filter(eq("id", 4)).iterator()
                .next();
        Assertions.assertEquals(1, result.loginCount);
        Assertions.assertEquals("Ron Swanson", result.username);
        Assertions.assertEquals(1, user.loginCount);
        Assertions.assertEquals("Ron Swanson", user.username);
    }

    @Test
    public void testIdUpdatedOnSave() {
        final Rectangle rect = new Rectangle(10, 10);
        getDs().save(rect);
        Assertions.assertNotNull(rect.getId());
    }

    @Test
    public void testInsert() {
        MongoCollection collection = getDs().getCollection(TestEntity.class);
        this.getDs().insert(new TestEntity());
        Assertions.assertEquals(1, collection.countDocuments());
        this.getDs().insert(new TestEntity(), new InsertOneOptions()
                .writeConcern(WriteConcern.ACKNOWLEDGED));
        Assertions.assertEquals(2, collection.countDocuments());
    }

    @Test
    public void testReplace() {
        User bob = new User("bob", LocalDate.now());
        User linda = new User("linda", LocalDate.now());

        Assertions.assertThrows(MissingIdException.class, () -> this.getDs().replace(bob));

        Assertions.assertThrows(MissingIdException.class, () -> this.getDs().replace(List.of(bob, linda)));

        this.getDs().insert(bob);
        Assertions.assertEquals(1, getDs().find(User.class).count());
        this.getDs().insert(linda);

        bob.setLikes(List.of("burgers"));
        getDs().replace(bob);
        Assertions.assertEquals(List.of("burgers"), getDs().find(User.class).first().getLikes());

        bob.setLikes(List.of("burgers", "linda"));
        linda.setLikes(List.of("bob", "tina", "gene", "louise"));
        getDs().replace(List.of(bob, linda));

        for (User user : getDs().find(User.class)) {
            if (user.name.equals("bob")) {
                Assertions.assertEquals(List.of("burgers", "linda"), user.getLikes());
            } else {
                Assertions.assertEquals(List.of("bob", "tina", "gene", "louise"), user.getLikes());
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
        Assertions.assertTrue(LifecycleListener.foundDatastore);
        Assertions.assertTrue(life1.prePersist);
        Assertions.assertTrue(life1.prePersistWithParam);
        Assertions.assertTrue(life1.prePersistWithParamAndReturn);
        Assertions.assertTrue(life1.postPersist);
        Assertions.assertTrue(life1.postPersistWithParam);

        final Datastore datastore = getDs();

        final LifecycleTestObj loaded = datastore.find(LifecycleTestObj.class)
                .filter(eq("_id", life1.id))
                .first();
        Assertions.assertTrue(loaded.preLoad);
        Assertions.assertTrue(loaded.preLoadWithParam);
        Assertions.assertTrue(loaded.postLoad);
        Assertions.assertTrue(loaded.postLoadWithParam);
    }

    @Test
    public void testLifecycleListeners() {
        final LifecycleTestObj life1 = new LifecycleTestObj();
        getDs().save(life1);
        Assertions.assertTrue(LifecycleListener.prePersist);
        Assertions.assertTrue(LifecycleListener.prePersistWithEntity);
    }

    @Test
    public void testModifyWithBadPaths() {
        Query<LifecycleTestObj> query = getDs().find(LifecycleTestObj.class);

        Assertions.assertThrows(ValidationException.class, () -> query.modify(inc("some.field", 2)));

        query.first();
    }

    @Test
    public void testRefresh() {
        FacebookUser steve = getDs().save(new FacebookUser(1, "Steve"));

        Assertions.assertEquals(0, steve.loginCount);
        UpdateResult loginCount = getDs().find(FacebookUser.class)
                .update(inc("loginCount", 10));

        Assertions.assertEquals(1, loginCount.getModifiedCount());

        getDs().refresh(steve);
        Assertions.assertEquals(10, steve.loginCount);

        loginCount = getDs().find(FacebookUser.class)
                .update(
                        set("username", "Mark"),
                        set("loginCount", 1));

        Assertions.assertEquals(1, loginCount.getModifiedCount());
        getDs().refresh(steve);
        Assertions.assertEquals(1, steve.loginCount);
        Assertions.assertEquals("Mark", steve.username);

    }

    @Test
    public void testSaveWithNoID() {
        Grade grade = new Grade();
        grade.marks = 80;

        Assertions.assertThrows(MappingException.class, () -> getDs().save(grade));

        Assertions.assertThrows(MappingException.class, () -> getDs().save(of(grade, grade)));

    }

    @Test
    public void testUpdateWithCollation() {
        getDs().save(asList(new FacebookUser(1, "John Doe"),
                new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                .filter(eq("username", "john doe"));

        UpdateResult results = query.update(inc("loginCount"));

        Assertions.assertEquals(1, results.getModifiedCount());
        Assertions.assertEquals(0, getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator().next().loginCount);
        Assertions.assertEquals(1, getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator()
                .next().loginCount);

        results = query.update(new UpdateOptions()
                .multi(true)
                .collation(Collation.builder()
                        .locale("en")
                        .collationStrength(SECONDARY)
                        .build()),
                inc("loginCount"));
        Assertions.assertEquals(2, results.getModifiedCount());
        Assertions.assertEquals(1, getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator()
                .next().loginCount);
        Assertions.assertEquals(2, getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator()
                .next().loginCount);
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
