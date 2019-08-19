/*
  Copyright (C) 2010 Olafur Gauti Gudmundsson
  <p/>
  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
  obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
  and limitations under the License.
 */

package dev.morphia;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.EntityListeners;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Transient;
import dev.morphia.generics.model.Child;
import dev.morphia.generics.model.ChildEntity;
import dev.morphia.query.FindAndDeleteOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Modify;
import dev.morphia.query.Query;
import dev.morphia.query.Update;
import dev.morphia.query.UpdateException;
import dev.morphia.testmodel.Address;
import dev.morphia.testmodel.Hotel;
import dev.morphia.testmodel.Rectangle;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.WriteConcern.ACKNOWLEDGED;
import static com.mongodb.WriteConcern.MAJORITY;
import static com.mongodb.client.model.ReturnDocument.AFTER;
import static com.mongodb.client.model.ReturnDocument.BEFORE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Scott Hernandez
 */
public class TestDatastore extends TestBase {

    @Test(expected = UpdateException.class)
    public void saveNull() {
        getDs().save((Hotel) null);
    }

    @Test
    public void shouldSaveGenericTypeVariables() {
        // given
        ChildEntity child = new ChildEntity();
        child.setEmbeddedList(singletonList(new Child()));

        // when
        getDs().save(child);

        // then
        assertNotNull(child.getId());
    }

    @Test
    public void testCollectionNames() {
        assertEquals("facebook_users", getMapper().getMappedClass(FacebookUser.class).getCollectionName());
    }

    @Test
    public void testDoesNotExistAfterDelete() {
        // given
        long id = System.currentTimeMillis();
        final long key = getDs().save(new FacebookUser(id, "user 1")).getId();

        // when
        getDs().find(FacebookUser.class).delete();

        // then
        assertNull("Shouldn't exist after delete", getDs().find(FacebookUser.class)
                                                          .filter("_id", key)
                                                          .first());
    }

    @Test
    public void testEmbedded() {
        getDs().find(Hotel.class).delete();
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
        assertEquals(1, getDs().find(Hotel.class).count());
        assertNotNull(borg.getId());

        final Hotel hotelLoaded = getDs().find(Hotel.class).filter("_id", borg.getId()).first();
        assertEquals(borg.getName(), hotelLoaded.getName());
        assertEquals(borg.getAddress().getPostCode(), hotelLoaded.getAddress().getPostCode());
    }

    @Test
    public void testGet() {
        getMapper().map(FacebookUser.class);
        List<FacebookUser> fbUsers = new ArrayList<>();
        fbUsers.add(new FacebookUser(1, "user 1"));
        fbUsers.add(new FacebookUser(2, "user 2"));
        fbUsers.add(new FacebookUser(3, "user 3"));
        fbUsers.add(new FacebookUser(4, "user 4"));

        getDs().save(fbUsers);
        assertEquals(4, getDs().find(FacebookUser.class).count());
        assertNotNull(getDs().find(FacebookUser.class).filter("_id", 1).first());
        List<FacebookUser> res = getDs().find(FacebookUser.class).filter("_id in", asList(1L, 2L)).execute().toList();
        assertEquals(2, res.size());
        assertNotNull(res.get(0));
        assertNotNull(res.get(1));
        assertNotNull(res.get(1).username);

        getDs().find(FacebookUser.class).remove();
        getAds().insert(fbUsers);
        assertEquals(4, getDs().find(FacebookUser.class).count());
        assertNotNull(getDs().find(FacebookUser.class).filter("_id", 1).first());
        res = getDs().find(FacebookUser.class).filter("_id in", asList(1L, 2L)).execute().toList();
        assertEquals(2, res.size());
        assertNotNull(res.get(0));
        assertNotNull(res.get(1));
        assertNotNull(res.get(1).username);
    }

    @Test
    public void testIdUpdatedOnSave() {
        final Rectangle rect = new Rectangle(10, 10);
        getDs().save(rect);
        assertNotNull(rect.getId());
    }

    @Test
    public void testLifecycle() {
        final LifecycleTestObj life1 = new LifecycleTestObj();
        getMapper().map(List.of(LifecycleTestObj.class));
        getDs().save(life1);
        assertTrue(life1.prePersist);
        assertTrue(life1.prePersistWithParam);
        assertTrue(life1.prePersistWithParamAndReturn);
        assertTrue(life1.postPersist);
        assertTrue(life1.postPersistWithParam);

        final Datastore datastore = getDs();

        final LifecycleTestObj loaded = datastore.find(LifecycleTestObj.class)
                                                 .filter("_id", life1.id)
                                                 .first();
        assertTrue(loaded.preLoad);
        assertTrue(loaded.preLoadWithParam);
        assertTrue(loaded.preLoadWithParamAndReturn);
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
    public void testMorphiaDS() {
        Morphia.createDatastore(getMongoClient(), "test");
    }

    @Test
    public void testMultipleDatabasesSingleThreaded() {
        getMapper().map(List.of(FacebookUser.class));

        final Datastore ds1 = Morphia.createDatastore(getMongoClient(), "db1");
        final Datastore ds2 = Morphia.createDatastore(getMongoClient(), "db2");

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

        testFirstDatastore(ds1);
        testSecondDatastore(ds2);

        testFirstDatastore(ds1);
        testSecondDatastore(ds2);

        testFirstDatastore(ds1);
        testSecondDatastore(ds2);

        testStandardDatastore();
    }

    @Test
    public void testSaveAndDelete() {
        getDs().getCollection(Rectangle.class).drop();

        final Rectangle rect = new Rectangle(10, 10);
        ObjectId id = new ObjectId();
        rect.setId(id);

        //test delete(entity)
        getDs().save(rect);
        assertEquals(1, getDs().find(rect.getClass()).count());
        getDs().delete(rect);
        assertEquals(0, getDs().find(rect.getClass()).count());

        //test delete(entity, id)
        getDs().save(rect);
        assertEquals(1, getDs().find(rect.getClass()).count());
        getDs().find(rect.getClass()).filter("_id", 1).delete();
        assertEquals(1, getDs().find(rect.getClass()).count());
        getDs().find(rect.getClass()).filter("_id", id).delete();
        assertEquals(0, getDs().find(rect.getClass()).count());

        //test delete(entity, {id})
        getDs().save(rect);
        assertEquals(1, getDs().find(rect.getClass()).count());
        getDs().find(rect.getClass()).filter("_id in", singletonList(rect.getId())).delete();
        assertEquals(0, getDs().find(rect.getClass()).count());

        //test delete(entity, {id,id})
        ObjectId id1 = getDs().save(new Rectangle(10, 10)).getId();
        ObjectId id2 = getDs().save(new Rectangle(10, 10)).getId();
        assertEquals(2, getDs().find(rect.getClass()).count());
        getDs().find(rect.getClass()).filter("_id in", asList(id1, id2)).delete();
        assertEquals(0, getDs().find(rect.getClass()).count());

        //test delete(Class, {id,id})
        id1 = getDs().save(new Rectangle(20, 20)).getId();
        id2 = getDs().save(new Rectangle(20, 20)).getId();
        assertEquals("datastore should have saved two entities with autogenerated ids", 2, getDs().find(rect.getClass()).count());
        getDs().find(rect.getClass()).filter("_id in", asList(id1, id2)).delete();
        assertEquals("datastore should have deleted two entities with autogenerated ids", 0, getDs().find(rect.getClass()).count());

        //test delete(entity, {id}) with one left
        id1 = getDs().save(new Rectangle(20, 20)).getId();
        getDs().save(new Rectangle(20, 20));
        assertEquals(2, getDs().find(rect.getClass()).count());
        getDs().find(rect.getClass()).filter("_id in", singletonList(id1)).delete();
        assertEquals(1, getDs().find(rect.getClass()).count());
        getDs().getCollection(Rectangle.class).drop();

        //test delete(Class, {id}) with one left
        id1 = getDs().save(new Rectangle(20, 20)).getId();
        getDs().save(new Rectangle(20, 20));
        assertEquals(2, getDs().find(rect.getClass()).count());
        getDs().find(Rectangle.class).filter("_id in", singletonList(id1)).delete();
        assertEquals(1, getDs().find(rect.getClass()).count());
    }

    @Test
    public void testUpdateWithCollation() {
        checkMinServerVersion(3.4);
        getDs().getCollection(FacebookUser.class).drop();
        getDs().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        final Update update = getDs().find(FacebookUser.class)
                                     .field("username").equal("john doe")
                                     .update()
                                     .inc("loginCount");

        UpdateResult results = update.execute();

        assertEquals(1, results.getModifiedCount());
        assertEquals(0, getDs().find(FacebookUser.class).filter("id", 1)
                               .execute(new FindOptions().limit(1)).next()
                            .loginCount);
        assertEquals(1, getDs().find(FacebookUser.class).filter("id", 2)
                               .execute(new FindOptions().limit(1))
                               .next()
                            .loginCount);

        results = update.execute(new UpdateOptions()
                                     .multi(true)
                                     .collation(Collation.builder()
                                                         .locale("en")
                                                         .collationStrength(CollationStrength.SECONDARY)
                                                         .build()));
        assertEquals(2, results.getModifiedCount());
        assertEquals(1, getDs().find(FacebookUser.class).filter("id", 1)
                               .execute(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(2, getDs().find(FacebookUser.class).filter("id", 2)
                               .execute(new FindOptions().limit(1))
                               .next()
                            .loginCount);
    }

    @Test
    public void testFindAndModify() {
        getDs().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                                           .field("username").equal("john doe");
        Modify<FacebookUser> modify = query.modify().inc("loginCount");
        FacebookUser results = modify.execute();

        assertEquals(0, getDs().find(FacebookUser.class).filter("id", 1)
                               .execute(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(1, getDs().find(FacebookUser.class).filter("id", 2)
                               .execute(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(1, results.loginCount);

        results = modify.execute(new FindAndModifyOptions().returnDocument(BEFORE));
        assertEquals(0, getDs().find(FacebookUser.class).filter("id", 1)
                               .execute(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(2, getDs().find(FacebookUser.class).filter("id", 2)
                               .execute(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(1, results.loginCount);

        query = getDs().find(FacebookUser.class)
                       .field("id").equal(3L)
                       .field("username").equal("Jon Snow");
        results = query.modify().inc("loginCount").execute(new FindAndModifyOptions().returnDocument(BEFORE).upsert(true));

        assertNull(results);
        FacebookUser user = getDs().find(FacebookUser.class).filter("id", 3)
                                   .execute(new FindOptions().limit(1))
                                   .next();
        assertEquals(1, user.loginCount);
        assertEquals("Jon Snow", user.username);


        query = getDs().find(FacebookUser.class)
                       .field("id").equal(4L)
                       .field("username").equal("Ron Swanson");
        results = query.modify().inc("loginCount").execute(new FindAndModifyOptions().returnDocument(AFTER).upsert(true));

        assertNotNull(results);
        user = getDs().find(FacebookUser.class).filter("id", 4)
                      .execute(new FindOptions().limit(1))
                      .next();
        assertEquals(1, results.loginCount);
        assertEquals("Ron Swanson", results.username);
        assertEquals(1, user.loginCount);
        assertEquals("Ron Swanson", user.username);
    }

    @Test
    public void testFindAndModifyWithOptions() {
        checkMinServerVersion(3.4);
        getDs().getCollection(FacebookUser.class).drop();
        getDs().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                                           .field("username").equal("john doe");
        Modify<FacebookUser> modify = query.modify()
                                           .inc("loginCount");
        FacebookUser results = modify.execute();

        assertEquals(0, getDs().find(FacebookUser.class).filter("id", 1)
                               .execute(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(1, getDs().find(FacebookUser.class).filter("id", 2)
                               .execute(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(1, results.loginCount);

        results = modify.execute(new FindAndModifyOptions()
                                     .returnDocument(BEFORE)
                                     .collation(Collation.builder()
                                                         .locale("en")
                                                         .collationStrength(CollationStrength.SECONDARY)
                                                         .build()));
        assertEquals(1, getDs().find(FacebookUser.class).filter("id", 1)
                               .execute(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(0, results.loginCount);
        assertEquals(1, getDs().find(FacebookUser.class).filter("id", 2)
                               .execute(new FindOptions().limit(1))
                               .next()
                            .loginCount);

        results = getDs().find(FacebookUser.class)
                         .field("id").equal(3L)
                         .field("username").equal("Jon Snow")
                         .modify()
                         .inc("loginCount")
                         .execute(new FindAndModifyOptions()
                                      .returnDocument(BEFORE)
                                      .upsert(true));

        assertNull(results);
        FacebookUser user = getDs().find(FacebookUser.class).filter("id", 3)
                                   .execute(new FindOptions().limit(1))
                                   .next();
        assertEquals(1, user.loginCount);
        assertEquals("Jon Snow", user.username);


        results = getDs().find(FacebookUser.class)
                         .field("id").equal(4L)
                         .field("username").equal("Ron Swanson")
                         .modify()
                         .inc("loginCount")
                         .execute(new FindAndModifyOptions().upsert(true));

        assertNotNull(results);
        user = getDs().find(FacebookUser.class).filter("id", 4)
                      .execute(new FindOptions().limit(1))
                      .next();
        assertEquals(1, results.loginCount);
        assertEquals("Ron Swanson", results.username);
        assertEquals(1, user.loginCount);
        assertEquals("Ron Swanson", user.username);
    }

    @Test
    public void testDeleteWithCollation() {
        checkMinServerVersion(3.4);
        getDs().getCollection(FacebookUser.class).drop();
        getDs().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                                           .field("username").equal("john doe");
        assertEquals(1, query.remove().getDeletedCount());

        assertEquals(1, query.remove(new DeleteOptions()
                                         .collation(Collation.builder()
                                                             .locale("en")
                                                             .collationStrength(CollationStrength.SECONDARY)
                                                             .build()))
                             .getDeletedCount());
    }

    @Test
    public void testEnforceWriteConcern() {
        DatastoreImpl ds = (DatastoreImpl) getDs();
        FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions();
        assertNull(findAndModifyOptions.getWriteConcern());

        MongoCollection<Document> dummy = getDatabase().getCollection("dummy");

        assertEquals(ACKNOWLEDGED, ds.enforceWriteConcern(dummy, FacebookUser.class, findAndModifyOptions.getWriteConcern())
                                     .getWriteConcern());
        findAndModifyOptions.writeConcern(MAJORITY);
        assertEquals(MAJORITY, ds.enforceWriteConcern(dummy, FacebookUser.class, findAndModifyOptions.getWriteConcern())
                                 .getWriteConcern());

        InsertOptions insertOptions = new InsertOptions();
        assertNull(insertOptions.getWriteConcern());

        UpdateOptions updateOptions = new UpdateOptions();
        assertNull(updateOptions.getWriteConcern());
    }

    @Test
    public void testFindAndDeleteWithCollation() {
        checkMinServerVersion(3.4);
        getDs().getCollection(FacebookUser.class).drop();
        getDs().save(asList(new FacebookUser(1, "John Doe"),
            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                                           .field("username").equal("john doe");
        assertNotNull(query.delete());
        assertNull(query.delete());

        FindAndDeleteOptions options = new FindAndDeleteOptions()
                                           .collation(Collation.builder()
                                                               .locale("en")
                                                               .collationStrength(CollationStrength.SECONDARY)
                                                               .build());
        assertNotNull(query.delete(options));
        assertNull(query.execute());
    }

    @Test
    public void testFindAndDeleteWithNoQueryMatch() {
        assertNull(getDs().find(FacebookUser.class)
                          .field("username").equal("David S. Pumpkins")
                          .delete());
    }

    private void testFirstDatastore(final Datastore ds1) {
        final FacebookUser user = ds1.find(FacebookUser.class).filter("id", 1)
                                     .execute(new FindOptions().limit(1))
                                     .next();
        Assert.assertNotNull(user);
        Assert.assertNotNull(ds1.find(FacebookUser.class).filter("id", 3)
                                .execute(new FindOptions().limit(1))
                                .next());

        Assert.assertEquals("Should find 1 friend", 1, user.friends.size());
        Assert.assertEquals("Should find the right friend", 3, user.friends.get(0).id);

        Assert.assertNull(ds1.find(FacebookUser.class).filter("id", 2)
                             .execute(new FindOptions().limit(1))
                             .tryNext());
        Assert.assertNull(ds1.find(FacebookUser.class).filter("id", 4)
                             .execute(new FindOptions().limit(1))
                             .tryNext());
    }

    private void testSecondDatastore(final Datastore ds2) {
        Assert.assertNull(ds2.find(FacebookUser.class).filter("id", 1)
                             .execute(new FindOptions().limit(1))
                             .tryNext());
        Assert.assertNull(ds2.find(FacebookUser.class).filter("id", 3)
                             .execute(new FindOptions().limit(1))
                             .tryNext());

        final FacebookUser db2FoundUser = ds2.find(FacebookUser.class).filter("id", 2)
                                             .execute(new FindOptions().limit(1))
                                             .next();
        Assert.assertNotNull(db2FoundUser);
        Assert.assertNotNull(ds2.find(FacebookUser.class).filter("id", 4)
                                .execute(new FindOptions().limit(1))
                                .next());
        Assert.assertEquals("Should find 1 friend", 1, db2FoundUser.friends.size());
        Assert.assertEquals("Should find the right friend", 4, db2FoundUser.friends.get(0).id);
    }

    private void testStandardDatastore() {
        Assert.assertNull(getDs().find(FacebookUser.class).filter("id", 1)
                                 .execute(new FindOptions().limit(1))
                                 .tryNext());
        Assert.assertNull(getDs().find(FacebookUser.class).filter("id", 2)
                                 .execute(new FindOptions().limit(1))
                                 .tryNext());
        Assert.assertNull(getDs().find(FacebookUser.class).filter("id", 3)
                                 .execute(new FindOptions().limit(1))
                                 .tryNext());
        Assert.assertNull(getDs().find(FacebookUser.class).filter("id", 4)
                                 .execute(new FindOptions().limit(1))
                                 .tryNext());
    }

    @Entity("facebook_users")
    public static class FacebookUser {
        @Id
        private long id;
        private String username;
        private int loginCount;
        @Reference
        private List<FacebookUser> friends = new ArrayList<>();

        public FacebookUser(final long id, final String name) {
            this();
            this.id = id;
            username = name;
        }

        public FacebookUser() {
        }

        public long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public int getLoginCount() {
            return loginCount;
        }
    }

    @Entity(value = "facebook_users", useDiscriminator = false)
    public static class FacebookUserWithNoClassNameStored extends FacebookUser {
        public FacebookUserWithNoClassNameStored(long id, String name) {
            super(id, name);
        }

        public FacebookUserWithNoClassNameStored() {
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class LifecycleListener {
        private static boolean prePersist;
        private static boolean prePersistWithEntity;

        @PrePersist
        void prePersist() {
            prePersist = true;
        }

        @PrePersist
        void prePersist(final LifecycleTestObj obj) {
            if (obj == null) {
                throw new RuntimeException();
            }
            prePersistWithEntity = true;

        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @Entity
    @EntityListeners(LifecycleListener.class)
    public static class LifecycleTestObj {
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
        public Document prePersistWithParamAndReturn(final Document document) {
            if (prePersistWithParamAndReturn) {
                throw new RuntimeException("already called");
            }
            prePersistWithParamAndReturn = true;
            return null;
        }

        @PrePersist
        protected void prePersistWithParam(final Document document) {
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

        @PrePersist
        void prePersist() {
            if (prePersist) {
                throw new RuntimeException("already called");
            }

            prePersist = true;
        }

        @PostPersist
        void postPersistWithParam(final Document document) {
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
        void preLoadWithParam(final Document document) {
            document.put("preLoadWithParam", true);
        }

        @PreLoad
        Document preLoadWithParamAndReturn(final Document document) {
            final Document retObj = new Document();
            retObj.putAll(document);
            retObj.put("preLoadWithParamAndReturn", true);
            return retObj;
        }

        @PostLoad
        void postLoad() {
            if (postLoad) {
                throw new RuntimeException("already called");
            }

            postLoad = true;
        }

        @PostLoad
        void postLoadWithParam(final Document document) {
            if (postLoadWithParam) {
                throw new RuntimeException("already called");
            }
            postLoadWithParam = true;
        }
    }

    @Entity
    @SuppressWarnings("UnusedDeclaration")
    public static class Keys {
        @Id
        private ObjectId id;
        private List<Key<FacebookUser>> users;
        private Key<Rectangle> rect;

        private Keys() {
        }

        public Keys(final Key<Rectangle> rectKey, final List<Key<FacebookUser>> users) {
            rect = rectKey;
            this.users = users;
        }

        public ObjectId getId() {
            return id;
        }

        public Key<Rectangle> getRect() {
            return rect;
        }

        public List<Key<FacebookUser>> getUsers() {
            return users;
        }
    }

    @Entity(concern = "ACKNOWLEDGED")
    static class Simple {
        @Id
        private String id;

        Simple(final String id) {
            this();
            this.id = id;
        }

        private Simple() {
        }
    }

}
