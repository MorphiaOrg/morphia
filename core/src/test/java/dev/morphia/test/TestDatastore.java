package dev.morphia.test;

import com.mongodb.client.model.Collation;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.ModifyOptions;
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
import dev.morphia.query.FindAndDeleteOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Modify;
import dev.morphia.query.Query;
import dev.morphia.query.Update;
import dev.morphia.test.models.City;
import dev.morphia.test.models.CurrentStatus;
import dev.morphia.test.models.FacebookUser;
import dev.morphia.test.models.Grade;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.util.List;

import static com.mongodb.client.model.CollationStrength.SECONDARY;
import static com.mongodb.client.model.ReturnDocument.AFTER;
import static com.mongodb.client.model.ReturnDocument.BEFORE;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.updates.UpdateOperators.inc;
import static dev.morphia.query.experimental.updates.UpdateOperators.set;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class TestDatastore extends TestBase {
    @Test
    public void testCappedEntity() {
        // given
        getMapper().map(CurrentStatus.class);
        getDs().ensureCaps();

        assertCapped(CurrentStatus.class, 1, 1048576);

        // when-then
        Query<CurrentStatus> query = getDs().find(CurrentStatus.class);

        getDs().save(new CurrentStatus("All Good"));
        assertEquals(query.count(), 1);

        getDs().save(new CurrentStatus("Kinda Bad"));
        assertEquals(query.count(), 1);

        assertTrue(query.iterator(new FindOptions().limit(1))
                        .next()
                       .message.contains("Bad"));

        getDs().save(new CurrentStatus("Kinda Bad2"));
        assertEquals(query.count(), 1);

        getDs().save(new CurrentStatus("Kinda Bad3"));
        assertEquals(query.count(), 1);

        getDs().save(new CurrentStatus("Kinda Bad4"));
        assertEquals(query.count(), 1);
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
    public void testFindAndModify() {
        getDs().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                                           .filter(eq("username", "john doe"));
        Modify<FacebookUser> modify = query.modify(inc("loginCount"));

        assertEquals(modify.execute().loginCount, 0);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 1)).first().loginCount, 0);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 2)).first().loginCount, 1);

        assertEquals(modify.execute(new ModifyOptions().returnDocument(AFTER)).loginCount, 2);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 1)).first().loginCount, 0);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 2)).first().loginCount, 2);

        assertNull(getDs().find(FacebookUser.class)
                          .filter(eq("id", 3L),
                              eq("username", "Jon Snow"))
                          .modify(inc("loginCount", 4))
                          .execute(new ModifyOptions()
                                       .returnDocument(BEFORE)
                                       .upsert(true)));

        FacebookUser user = getDs().find(FacebookUser.class).filter(eq("id", 3)).first();
        assertEquals(user.loginCount, 4);
        assertEquals(user.username, "Jon Snow");

        FacebookUser results = getDs().find(FacebookUser.class)
                                      .filter(eq("id", 4L),
                                          eq("username", "Ron Swanson"))
                                      .modify(inc("loginCount"))
                                      .execute(new ModifyOptions()
                                                   .returnDocument(AFTER)
                                                   .upsert(true));
        assertEquals(results.loginCount, 1);
        assertEquals(results.username, "Ron Swanson");

        user = getDs().find(FacebookUser.class).filter(eq("id", 4)).iterator(new FindOptions().limit(1)).next();
        assertEquals(user.loginCount, 1);
        assertEquals(user.username, "Ron Swanson");
    }

    @Test
    public void testFindAndModifyWithOptions() {
        getDs().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        FacebookUser result = getDs().find(FacebookUser.class)
                                     .filter(eq("username", "john doe"))
                                     .modify(inc("loginCount"))
                                     .execute();

        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator(new FindOptions().limit(1))
                            .next()
                         .loginCount, 0);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator(new FindOptions().limit(1))
                            .next()
                         .loginCount, 1);
        assertEquals(result.loginCount, 0);

        result = getDs().find(FacebookUser.class)
                        .filter(eq("username", "john doe"))
                        .modify(inc("loginCount"))
                        .execute(new ModifyOptions()
                                     .returnDocument(BEFORE)
                                     .collation(Collation.builder()
                                                         .locale("en")
                                                         .collationStrength(SECONDARY)
                                                         .build()));
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator(new FindOptions().limit(1))
                            .next()
                         .loginCount, 1);
        assertEquals(result.loginCount, 0);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator(new FindOptions().limit(1))
                            .next()
                         .loginCount, 1);

        result = getDs().find(FacebookUser.class)
                        .filter(eq("id", 3L),
                            eq("username", "Jon Snow"))
                        .modify(inc("loginCount"))
                        .execute(new ModifyOptions()
                                     .returnDocument(BEFORE)
                                     .upsert(true));

        assertNull(result);
        FacebookUser user = getDs().find(FacebookUser.class).filter(eq("id", 3)).iterator(new FindOptions().limit(1))
                                   .next();
        assertEquals(user.loginCount, 1);
        assertEquals(user.username, "Jon Snow");


        result = getDs().find(FacebookUser.class)
                        .filter(eq("id", 4L),
                            eq("username", "Ron Swanson"))
                        .modify(inc("loginCount"))
                        .execute(new ModifyOptions()
                                     .returnDocument(AFTER)
                                     .upsert(true));

        assertNotNull(result);
        user = getDs().find(FacebookUser.class).filter(eq("id", 4)).iterator(new FindOptions().limit(1))
                      .next();
        assertEquals(result.loginCount, 1);
        assertEquals(result.username, "Ron Swanson");
        assertEquals(user.loginCount, 1);
        assertEquals(user.username, "Ron Swanson");
    }

    @Test
    public void testLifecycle() {
        final LifecycleTestObj life1 = new LifecycleTestObj();
        getMapper().map(List.of(LifecycleTestObj.class));
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
        getMapper().map(List.of(LifecycleTestObj.class));
        getDs().save(life1);
        assertTrue(LifecycleListener.prePersist);
        assertTrue(LifecycleListener.prePersistWithEntity);
    }

    @Test
    public void testRefresh() {
        FacebookUser steve = getDs().save(new FacebookUser(1, "Steve"));

        assertEquals(steve.loginCount, 0);
        UpdateResult loginCount = getDs().find(FacebookUser.class)
                                         .update(inc("loginCount", 10))
                                         .execute();

        assertEquals(loginCount.getModifiedCount(), 1);

        getDs().refresh(steve);
        assertEquals(steve.loginCount, 10);

        loginCount = getDs().find(FacebookUser.class)
                            .update(
                                set("username", "Mark"),
                                set("loginCount", 1))
                            .execute();

        assertEquals(loginCount.getModifiedCount(), 1);
        getDs().refresh(steve);
        assertEquals(steve.loginCount, 1);
        assertEquals(steve.username, "Mark");

    }

    @Test
    public void testSaveWithNoID() {
        getMapper().map(Grade.class);
        Grade grade = new Grade();
        grade.marks = 80;

        assertThrows(MappingException.class, () -> {
            getDs().save(grade);
        });

        assertThrows(MappingException.class, () -> {
            getDs().save(List.of(grade, grade));
        });

    }

    @Test
    public void testUpdateWithCollation() {
        getDs().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        final Update<FacebookUser> update = getDs().find(FacebookUser.class)
                                                   .filter(eq("username", "john doe"))
                                                   .update(inc("loginCount"));

        UpdateResult results = update.execute();

        assertEquals(results.getModifiedCount(), 1);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator(new FindOptions().limit(1)).next()
                         .loginCount, 0);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator(new FindOptions().limit(1))
                            .next()
                         .loginCount, 1);

        results = update.execute(new UpdateOptions()
                                     .multi(true)
                                     .collation(Collation.builder()
                                                         .locale("en")
                                                         .collationStrength(SECONDARY)
                                                         .build()));
        assertEquals(results.getModifiedCount(), 2);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 1)).iterator(new FindOptions().limit(1))
                            .next()
                         .loginCount, 1);
        assertEquals(getDs().find(FacebookUser.class).filter(eq("id", 2)).iterator(new FindOptions().limit(1))
                            .next()
                         .loginCount, 2);
    }

    private static class LifecycleListener {
        private static boolean prePersist;
        private static boolean prePersistWithEntity;
        private static boolean foundDatastore;

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

    @SuppressWarnings("UnusedDeclaration")
    @Entity
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
