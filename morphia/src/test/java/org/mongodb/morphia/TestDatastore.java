/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package org.mongodb.morphia;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.EntityListeners;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.PostPersist;
import org.mongodb.morphia.annotations.PreLoad;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.generics.model.ChildEmbedded;
import org.mongodb.morphia.generics.model.ChildEntity;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.UpdateException;
import org.mongodb.morphia.testmodel.Address;
import org.mongodb.morphia.testmodel.Hotel;
import org.mongodb.morphia.testmodel.Rectangle;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.mongodb.ReadPreference.secondaryPreferred;
import static com.mongodb.WriteConcern.REPLICA_ACKNOWLEDGED;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * @author Scott Hernandez
 */
//@RunWith(Parameterized.class)
public class TestDatastore extends TestBase {

    @Test(expected = UpdateException.class)
    public void saveNull() {
        getDs().save((Hotel) null);
    }


    @Test
    public void shouldSaveGenericTypeVariables() throws Exception {
        // given
        ChildEntity child = new ChildEntity();
        child.setEmbeddedList(singletonList(new ChildEmbedded()));

        // when
        Key<ChildEntity> saveResult = getDs().save(child);

        // then
        assertNotEquals(null, saveResult);
    }

    @Test
    public void testCollectionNames() throws Exception {
        assertEquals("facebook_users", getMorphia().getMapper().getCollectionName(FacebookUser.class));
    }

    @Test
    public void testDoesNotExistAfterDelete() throws Exception {
        // given
        long id = System.currentTimeMillis();
        final Key<FacebookUser> key = getDs().save(new FacebookUser(id, "user 1"));

        // when
        getDs().delete(getDs().find(FacebookUser.class));

        // then
        assertNull("Shouldn't exist after delete", getDs().exists(key));
    }

    @Test
    public void testEmbedded() throws Exception {
        getDs().delete(getDs().createQuery(Hotel.class));
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
        assertEquals(1, getDs().getCount(Hotel.class));
        assertNotNull(borg.getId());

        final Hotel hotelLoaded = getDs().get(Hotel.class, borg.getId());
        assertEquals(borg.getName(), hotelLoaded.getName());
        assertEquals(borg.getAddress().getPostCode(), hotelLoaded.getAddress().getPostCode());
    }

    @Test
    public void testExistsWhenItemSaved() throws Exception {
        // given
        long id = System.currentTimeMillis();
        final Key<FacebookUser> key = getDs().save(new FacebookUser(id, "user 1"));

        // expect
        assertNotNull(getDs().get(FacebookUser.class, id));
        assertNotNull(getDs().exists(key));
    }

    @Test
    public void testExistsWhenSecondaryPreferred() throws Exception {
        assumeTrue(isReplicaSet());

        // given
        long id = System.currentTimeMillis();
        final Key<FacebookUser> key = getDs().save(new FacebookUser(id, "user 1"), REPLICA_ACKNOWLEDGED);

        // expect
        assertNotNull("Should exist when using secondaryPreferred", getAds().exists(key, secondaryPreferred()));
    }

    @Test
    public void testExistsWithEntity() throws Exception {
        final FacebookUser facebookUser = new FacebookUser(1, "user one");
        getDs().save(facebookUser);
        assertEquals(1, getDs().getCount(FacebookUser.class));
        assertNotNull(getDs().get(FacebookUser.class, 1));
        assertNotNull(getDs().exists(facebookUser));
        getDs().delete(getDs().find(FacebookUser.class));
        assertEquals(0, getDs().getCount(FacebookUser.class));
        assertNull(getDs().exists(facebookUser));
    }

    @Test
    public void testGet() throws Exception {
        getMorphia().map(FacebookUser.class);
        final List<FacebookUser> fbUsers = new ArrayList<FacebookUser>();
        fbUsers.add(new FacebookUser(1, "user 1"));
        fbUsers.add(new FacebookUser(2, "user 2"));
        fbUsers.add(new FacebookUser(3, "user 3"));
        fbUsers.add(new FacebookUser(4, "user 4"));

        getDs().save(fbUsers);
        assertEquals(4, getDs().getCount(FacebookUser.class));
        assertNotNull(getDs().get(FacebookUser.class, 1));
        final List<Long> ids = new ArrayList<Long>(2);
        ids.add(1L);
        ids.add(2L);
        final List<FacebookUser> res = getDs().get(FacebookUser.class, ids).asList();
        assertEquals(2, res.size());
        assertNotNull(res.get(0));
        assertNotNull(res.get(0).id);
        assertNotNull(res.get(1));
        assertNotNull(res.get(1).username);
    }

    @Test
    public void testIdUpdatedOnSave() throws Exception {
        final Rectangle rect = new Rectangle(10, 10);
        getDs().save(rect);
        assertNotNull(rect.getId());
    }

    @Test
    public void testLifecycle() throws Exception {
        final LifecycleTestObj life1 = new LifecycleTestObj();
        getMorphia().getMapper().addMappedClass(LifecycleTestObj.class);
        getDs().save(life1);
        assertTrue(life1.prePersist);
        assertTrue(life1.prePersistWithParam);
        assertTrue(life1.prePersistWithParamAndReturn);
        assertTrue(life1.postPersist);
        assertTrue(life1.postPersistWithParam);

        final LifecycleTestObj loaded = getDs().get(life1);
        assertTrue(loaded.preLoad);
        assertTrue(loaded.preLoadWithParam);
        assertTrue(loaded.preLoadWithParamAndReturn);
        assertTrue(loaded.postLoad);
        assertTrue(loaded.postLoadWithParam);
    }

    @Test
    public void testLifecycleListeners() throws Exception {
        final LifecycleTestObj life1 = new LifecycleTestObj();
        getMorphia().getMapper().addMappedClass(LifecycleTestObj.class);
        getDs().save(life1);
        assertTrue(LifecycleListener.prePersist);
        assertTrue(LifecycleListener.prePersistWithEntity);
    }

    @Test
    public void testMorphiaDS() throws Exception {
        new Morphia().createDatastore(getMongoClient(), "test");
    }

    @Test
    public void testMultipleDatabasesSingleThreaded() throws InterruptedException, TimeoutException, ExecutionException {
        getMorphia().map(FacebookUser.class);

        final Datastore ds1 = getMorphia().createDatastore(getMongoClient(), "db1");
        final Datastore ds2 = getMorphia().createDatastore(getMongoClient(), "db2");

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
        assertEquals(1, getDs().getCount(rect));
        getDs().delete(rect);
        assertEquals(0, getDs().getCount(rect));

        //test delete(entity, id)
        getDs().save(rect);
        assertEquals(1, getDs().getCount(rect));
        getDs().delete(rect.getClass(), 1);
        assertEquals(1, getDs().getCount(rect));
        getDs().delete(rect.getClass(), id);
        assertEquals(0, getDs().getCount(rect));

        //test delete(entity, {id})
        getDs().save(rect);
        assertEquals(1, getDs().getCount(rect));
        getDs().delete(rect.getClass(), singletonList(rect.getId()));
        assertEquals(0, getDs().getCount(rect));

        //test delete(entity, {id,id})
        ObjectId id1 = (ObjectId) getDs().save(new Rectangle(10, 10)).getId();
        ObjectId id2 = (ObjectId) getDs().save(new Rectangle(10, 10)).getId();
        assertEquals(2, getDs().getCount(rect));
        getDs().delete(rect.getClass(), asList(id1, id2));
        assertEquals(0, getDs().getCount(rect));

        //test delete(Class, {id,id})
        id1 = (ObjectId) getDs().save(new Rectangle(20, 20)).getId();
        id2 = (ObjectId) getDs().save(new Rectangle(20, 20)).getId();
        assertEquals("datastore should have saved two entities with autogenerated ids", 2, getDs().getCount(rect));
        getDs().delete(rect.getClass(), asList(id1, id2));
        assertEquals("datastore should have deleted two entities with autogenerated ids", 0, getDs().getCount(rect));

        //test delete(entity, {id}) with one left
        id1 = (ObjectId) getDs().save(new Rectangle(20, 20)).getId();
        getDs().save(new Rectangle(20, 20));
        assertEquals(2, getDs().getCount(rect));
        getDs().delete(rect.getClass(), singletonList(id1));
        assertEquals(1, getDs().getCount(rect));
        getDs().getCollection(Rectangle.class).drop();

        //test delete(Class, {id}) with one left
        id1 = (ObjectId) getDs().save(new Rectangle(20, 20)).getId();
        getDs().save(new Rectangle(20, 20));
        assertEquals(2, getDs().getCount(rect));
        getDs().delete(Rectangle.class, singletonList(id1));
        assertEquals(1, getDs().getCount(rect));
    }

    private void testFirstDatastore(final Datastore ds1) {
        final FacebookUser user = ds1.find(FacebookUser.class, "id", 1).get();
        Assert.assertNotNull(user);
        Assert.assertNotNull(ds1.find(FacebookUser.class, "id", 3).get());

        Assert.assertEquals("Should find 1 friend", 1, user.friends.size());
        Assert.assertEquals("Should find the right friend", 3, user.friends.get(0).id);

        Assert.assertNull(ds1.find(FacebookUser.class, "id", 2).get());
        Assert.assertNull(ds1.find(FacebookUser.class, "id", 4).get());
    }

    private void testSecondDatastore(final Datastore ds2) {
        Assert.assertNull(ds2.find(FacebookUser.class, "id", 1).get());
        Assert.assertNull(ds2.find(FacebookUser.class, "id", 3).get());

        final FacebookUser db2FoundUser = ds2.find(FacebookUser.class, "id", 2).get();
        Assert.assertNotNull(db2FoundUser);
        Assert.assertNotNull(ds2.find(FacebookUser.class, "id", 4).get());
        Assert.assertEquals("Should find 1 friend", 1, db2FoundUser.friends.size());
        Assert.assertEquals("Should find the right friend", 4, db2FoundUser.friends.get(0).id);
    }

    private void testStandardDatastore() {
        Assert.assertNull(getDs().find(FacebookUser.class, "id", 1).get());
        Assert.assertNull(getDs().find(FacebookUser.class, "id", 2).get());
        Assert.assertNull(getDs().find(FacebookUser.class, "id", 3).get());
        Assert.assertNull(getDs().find(FacebookUser.class, "id", 4).get());
    }

    @Entity("facebook_users")
    public static class FacebookUser {
        @Id
        private long id;
        private String username;
        @Reference
        private List<FacebookUser> friends = new ArrayList<FacebookUser>();

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
        public DBObject prePersistWithParamAndReturn(final DBObject dbObj) {
            if (prePersistWithParamAndReturn) {
                throw new RuntimeException("already called");
            }
            prePersistWithParamAndReturn = true;
            return null;
        }

        @PrePersist
        protected void prePersistWithParam(final DBObject dbObj) {
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
        void postPersistWithParam(final DBObject dbObj) {
            postPersistWithParam = true;
            if (!dbObj.containsField(Mapper.ID_KEY)) {
                throw new RuntimeException("missing " + Mapper.ID_KEY);
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
        void preLoadWithParam(final DBObject dbObj) {
            dbObj.put("preLoadWithParam", true);
        }

        @PreLoad
        DBObject preLoadWithParamAndReturn(final DBObject dbObj) {
            final BasicDBObject retObj = new BasicDBObject();
            retObj.putAll(dbObj);
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

        @PreLoad
        void postLoadWithParam(final DBObject dbObj) {
            if (postLoadWithParam) {
                throw new RuntimeException("already called");
            }
            postLoadWithParam = true;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class KeysKeysKeys {
        @Id
        private ObjectId id;
        private List<Key<FacebookUser>> users;
        private Key<Rectangle> rect;

        private KeysKeysKeys() {
        }

        public KeysKeysKeys(final Key<Rectangle> rectKey, final List<Key<FacebookUser>> users) {
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

}
