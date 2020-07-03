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

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.EntityListeners;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Transient;
import dev.morphia.generics.model.ChildEmbedded;
import dev.morphia.generics.model.ChildEntity;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateException;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.UpdateResults;
import dev.morphia.testmodel.Address;
import dev.morphia.testmodel.Hotel;
import dev.morphia.testmodel.Rectangle;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.ReadPreference.secondaryPreferred;
import static com.mongodb.WriteConcern.ACKNOWLEDGED;
import static com.mongodb.WriteConcern.MAJORITY;
import static com.mongodb.WriteConcern.W2;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    @SuppressWarnings("deprecation")
    public void saveVarargs() {
        Iterable<Key<FacebookUser>> keys = getDs().save(new FacebookUser(1, "user 1"),
                                                        new FacebookUser(2, "user 2"),
                                                        new FacebookUser(3, "user 3"),
                                                        new FacebookUser(4, "user 4"));
        long id = 1;
        for (final Key<FacebookUser> key : keys) {
            assertEquals(id++, key.getId());
        }
        assertEquals(5, id);
        assertEquals(4, getDs().getCount(FacebookUser.class));

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
    public void testCollectionNames() {
        assertEquals("facebook_users", getMorphia().getMapper().getCollectionName(FacebookUser.class));
    }

    @Test
    public void testDoesNotExistAfterDelete() {
        // given
        long id = System.currentTimeMillis();
        final Key<FacebookUser> key = getDs().save(new FacebookUser(id, "user 1"));

        // when
        getDs().delete(getDs().find(FacebookUser.class));

        // then
        assertNull("Shouldn't exist after delete", getDs().exists(key));
    }

    @Test
    public void testEmbedded() {
        getDs().delete(getDs().find(Hotel.class));
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
    public void testExistsWhenItemSaved() {
        // given
        long id = System.currentTimeMillis();
        final Key<FacebookUser> key = getDs().save(new FacebookUser(id, "user 1"));

        // expect
        assertNotNull(getDs().get(FacebookUser.class, id));
        assertNotNull(getDs().exists(key));
    }

    @Test
    @SuppressWarnings("deprecation")
    @Ignore
    public void testExistsWhenSecondaryPreferredOld() {
        if (isReplicaSet()) {
            final Key<FacebookUser> key = getDs().save(new FacebookUser(System.currentTimeMillis(), "user 1"), W2);
            assertNotNull("Should exist when using secondaryPreferred", getAds().exists(key, secondaryPreferred()));
        }
    }

    @Test
    @Ignore
    public void testExistsWhenSecondaryPreferred() {
        if (isReplicaSet()) {
            final Key<FacebookUser> key = getDs().save(new FacebookUser(System.currentTimeMillis(), "user 1"),
                                                       new InsertOptions().writeConcern(W2));
            assertNotNull("Should exist when using secondaryPreferred", getAds().exists(key, secondaryPreferred()));
        }
    }


    @Test
    public void testExistsWithEntity() {
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
    public void testGet() {
        getMorphia().map(FacebookUser.class);
        List<FacebookUser> fbUsers = new ArrayList<FacebookUser>();
        fbUsers.add(new FacebookUser(1, "user 1"));
        fbUsers.add(new FacebookUser(2, "user 2"));
        fbUsers.add(new FacebookUser(3, "user 3"));
        fbUsers.add(new FacebookUser(4, "user 4"));

        getDs().save(fbUsers);
        assertEquals(4, getDs().getCount(FacebookUser.class));
        assertNotNull(getDs().get(FacebookUser.class, 1));
        List<FacebookUser> res = toList(getDs().get(FacebookUser.class, asList(1L, 2L)).find());
        assertEquals(2, res.size());
        assertNotNull(res.get(0));
        assertNotNull(res.get(0).id);
        assertNotNull(res.get(1));
        assertNotNull(res.get(1).username);

        getDs().getCollection(FacebookUser.class).remove(new BasicDBObject());
        getAds().insert(fbUsers);
        assertEquals(4, getDs().getCount(FacebookUser.class));
        assertNotNull(getDs().get(FacebookUser.class, 1));
        res = toList(getDs().get(FacebookUser.class, asList(1L, 2L)).find());
        assertEquals(2, res.size());
        assertNotNull(res.get(0));
        assertNotNull(res.get(0).id);
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
    public void testLifecycleListeners() {
        final LifecycleTestObj life1 = new LifecycleTestObj();
        getMorphia().getMapper().addMappedClass(LifecycleTestObj.class);
        getDs().save(life1);
        assertTrue(LifecycleListener.prePersist);
        assertTrue(LifecycleListener.prePersistWithEntity);
    }

    @Test
    public void testMorphiaDS() {
        new Morphia().createDatastore(getMongoClient(), "test");
    }

    @Test
    public void testMultipleDatabasesSingleThreaded() {
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

    @Test
    public void testUpdateWithCollation() {
        assumeMinServerVersion(3.4);
        getDs().getCollection(FacebookUser.class).drop();
        getDs().save(asList(new FacebookUser(1, "John Doe"),
                            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                                           .field("username").equal("john doe");
        UpdateOperations<FacebookUser> updateOperations = getDs().createUpdateOperations(FacebookUser.class)
            .inc("loginCount");
        UpdateResults results = getDs().update(query, updateOperations);
        assertEquals(1, results.getUpdatedCount());
        assertEquals(0, getDs().find(FacebookUser.class).filter("id", 1)
                               .find(new FindOptions().limit(1)).next()
                            .loginCount);
        assertEquals(1, getDs().find(FacebookUser.class).filter("id", 2)
                               .find(new FindOptions().limit(1))
                               .next()
                            .loginCount);

        results = getDs().update(query, updateOperations, new UpdateOptions()
            .multi(true)
            .collation(Collation.builder()
                                .locale("en")
                                .collationStrength(CollationStrength.SECONDARY)
                                .build()));
        assertEquals(2, results.getUpdatedCount());
        assertEquals(1, getDs().find(FacebookUser.class).filter("id", 1)
                               .find(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(2, getDs().find(FacebookUser.class).filter("id", 2)
                               .find(new FindOptions().limit(1))
                               .next()
                            .loginCount);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testFindAndModifyOld() {
        getDs().getCollection(FacebookUser.class).drop();
        getDs().save(asList(new FacebookUser(1, "John Doe"),
                            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                                           .field("username").equal("john doe");
        UpdateOperations<FacebookUser> updateOperations = getDs().createUpdateOperations(FacebookUser.class)
            .inc("loginCount");
        FacebookUser results = getDs().findAndModify(query, updateOperations);
        assertEquals(0, getDs().find(FacebookUser.class).filter("id", 1).get().loginCount);
        assertEquals(1, getDs().find(FacebookUser.class).filter("id", 2).get().loginCount);
        assertEquals(1, results.loginCount);

        results = getDs().findAndModify(query, updateOperations, true);
        assertEquals(0, getDs().find(FacebookUser.class).filter("id", 1).get().loginCount);
        assertEquals(2, getDs().find(FacebookUser.class).filter("id", 2).get().loginCount);
        assertEquals(1, results.loginCount);

        results = getDs().findAndModify(getDs().find(FacebookUser.class)
                                               .field("id").equal(3L)
                                               .field("username").equal("Jon Snow"), updateOperations, true, true);
        assertNull(results);
        FacebookUser user = getDs().find(FacebookUser.class).filter("id", 3).get();
        assertEquals(1, user.loginCount);
        assertEquals("Jon Snow", user.username);


        results = getDs().findAndModify(getDs().find(FacebookUser.class)
                                               .field("id").equal(4L)
                                               .field("username").equal("Ron Swanson"), updateOperations, false, true);
        assertNotNull(results);
        user = getDs().find(FacebookUser.class).filter("id", 4).get();
        assertEquals(1, results.loginCount);
        assertEquals("Ron Swanson", results.username);
        assertEquals(1, user.loginCount);
        assertEquals("Ron Swanson", user.username);
    }

    @Test
    public void testFindAndModify() {
        getDs().getCollection(FacebookUser.class).drop();
        getDs().save(asList(new FacebookUser(1, "John Doe"),
                            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                                           .field("username").equal("john doe");
        UpdateOperations<FacebookUser> updateOperations = getDs().createUpdateOperations(FacebookUser.class)
            .inc("loginCount");
        FacebookUser results = getDs().findAndModify(query, updateOperations);
        assertEquals(0, getDs().find(FacebookUser.class).filter("id", 1)
                               .find(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(1, getDs().find(FacebookUser.class).filter("id", 2)
                               .find(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(1, results.loginCount);

        results = getDs().findAndModify(query, updateOperations, new FindAndModifyOptions()
            .returnNew(false));
        assertEquals(0, getDs().find(FacebookUser.class).filter("id", 1)
                               .find(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(2, getDs().find(FacebookUser.class).filter("id", 2)
                               .find(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(1, results.loginCount);

        results = getDs().findAndModify(getDs().find(FacebookUser.class)
                                               .field("id").equal(3L)
                                               .field("username").equal("Jon Snow"), updateOperations, new FindAndModifyOptions()
                                            .returnNew(false)
                                            .upsert(true));
        assertNull(results);
        FacebookUser user = getDs().find(FacebookUser.class).filter("id", 3)
                                   .find(new FindOptions().limit(1))
                                   .next();
        assertEquals(1, user.loginCount);
        assertEquals("Jon Snow", user.username);


        results = getDs().findAndModify(getDs().find(FacebookUser.class)
                                               .field("id").equal(4L)
                                               .field("username").equal("Ron Swanson"), updateOperations, new FindAndModifyOptions()
                                                                                           .returnNew(true)
                                                                                           .upsert(true));
        assertNotNull(results);
        user = getDs().find(FacebookUser.class).filter("id", 4)
                      .find(new FindOptions().limit(1))
                      .next();
        assertEquals(1, results.loginCount);
        assertEquals("Ron Swanson", results.username);
        assertEquals(1, user.loginCount);
        assertEquals("Ron Swanson", user.username);
    }

    @Test
    public void testFindAndModifyWithOptions() {
        assumeMinServerVersion(3.4);
        getDs().getCollection(FacebookUser.class).drop();
        getDs().save(asList(new FacebookUser(1, "John Doe"),
                            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                                           .field("username").equal("john doe");
        UpdateOperations<FacebookUser> updateOperations = getDs().createUpdateOperations(FacebookUser.class)
            .inc("loginCount");
        FacebookUser results = getDs().findAndModify(query, updateOperations, new FindAndModifyOptions());
        assertEquals(0, getDs().find(FacebookUser.class).filter("id", 1)
                               .find(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(1, getDs().find(FacebookUser.class).filter("id", 2)
                               .find(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(1, results.loginCount);

        results = getDs().findAndModify(query, updateOperations, new FindAndModifyOptions()
            .returnNew(false)
            .collation(Collation.builder()
                                .locale("en")
                                .collationStrength(CollationStrength.SECONDARY)
                                .build()));
        assertEquals(1, getDs().find(FacebookUser.class).filter("id", 1)
                               .find(new FindOptions().limit(1))
                               .next()
                            .loginCount);
        assertEquals(0, results.loginCount);
        assertEquals(1, getDs().find(FacebookUser.class).filter("id", 2)
                               .find(new FindOptions().limit(1))
                               .next()
                            .loginCount);

        results = getDs().findAndModify(getDs().find(FacebookUser.class)
                                               .field("id").equal(3L)
                                               .field("username").equal("Jon Snow"),
                                        updateOperations, new FindAndModifyOptions()
                                            .returnNew(false)
                                            .upsert(true));
        assertNull(results);
        FacebookUser user = getDs().find(FacebookUser.class).filter("id", 3)
                                   .find(new FindOptions().limit(1))
                                   .next();
        assertEquals(1, user.loginCount);
        assertEquals("Jon Snow", user.username);


        results = getDs().findAndModify(getDs().find(FacebookUser.class)
                                               .field("id").equal(4L)
                                               .field("username").equal("Ron Swanson"),
                                        updateOperations, new FindAndModifyOptions()
                                            .upsert(true));
        assertNotNull(results);
        user = getDs().find(FacebookUser.class).filter("id", 4)
                      .find(new FindOptions().limit(1))
                      .next();
        assertEquals(1, results.loginCount);
        assertEquals("Ron Swanson", results.username);
        assertEquals(1, user.loginCount);
        assertEquals("Ron Swanson", user.username);
    }

    @Test
    public void testDeleteWithCollation() {
        assumeMinServerVersion(3.4);
        getDs().getCollection(FacebookUser.class).drop();
        getDs().save(asList(new FacebookUser(1, "John Doe"),
                            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                                           .field("username").equal("john doe");
        assertEquals(1, getDs().delete(query).getN());

        assertEquals(1, getDs().delete(query, new DeleteOptions()
            .collation(Collation.builder()
                                .locale("en")
                                .collationStrength(CollationStrength.SECONDARY)
                                .build()))
                               .getN());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testEnforceWriteConcern() {
        DatastoreImpl ds = (DatastoreImpl) getDs();
        FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions();
        assertNull(findAndModifyOptions.getWriteConcern());

        assertEquals(ACKNOWLEDGED, ds.enforceWriteConcern(findAndModifyOptions, FacebookUser.class)
                                     .getWriteConcern());
        assertEquals(MAJORITY, ds.enforceWriteConcern(findAndModifyOptions.writeConcern(MAJORITY), FacebookUser.class)
                                 .getWriteConcern());

        InsertOptions insertOptions = new InsertOptions();
        assertNull(insertOptions.getWriteConcern());

        assertEquals(ACKNOWLEDGED, ds.enforceWriteConcern(insertOptions, FacebookUser.class)
                                     .getWriteConcern());
        assertEquals(MAJORITY, ds.enforceWriteConcern(insertOptions.writeConcern(MAJORITY), FacebookUser.class)
                                 .getWriteConcern());

        UpdateOptions updateOptions = new UpdateOptions();
        assertNull(updateOptions.getWriteConcern());

        assertEquals(ACKNOWLEDGED, ds.enforceWriteConcern(updateOptions, FacebookUser.class)
                                     .getWriteConcern());
        assertEquals(MAJORITY, ds.enforceWriteConcern(updateOptions.writeConcern(MAJORITY), FacebookUser.class)
                                 .getWriteConcern());

        DeleteOptions deleteOptions = new DeleteOptions();
        assertNull(deleteOptions.getWriteConcern());

        assertEquals(ACKNOWLEDGED, ds.enforceWriteConcern(deleteOptions, FacebookUser.class)
                                     .getWriteConcern());
        assertEquals(MAJORITY, ds.enforceWriteConcern(deleteOptions.writeConcern(MAJORITY), FacebookUser.class)
                                 .getWriteConcern());
    }

    @Test
    public void entityWriteConcern() {
        ensureEntityWriteConcern();

        getDs().setDefaultWriteConcern(WriteConcern.UNACKNOWLEDGED);
        ensureEntityWriteConcern();
    }

    @SuppressWarnings("deprecation")
    private void ensureEntityWriteConcern() {
        DatastoreImpl datastore = (DatastoreImpl) getAds();
        assertEquals(ACKNOWLEDGED, datastore.enforceWriteConcern(new InsertOptions(), Simple.class)
                                            .getWriteConcern());
        assertEquals(ACKNOWLEDGED, datastore.enforceWriteConcern(new UpdateOptions(), Simple.class)
                                            .getWriteConcern());
        assertEquals(ACKNOWLEDGED, datastore.enforceWriteConcern(new FindAndModifyOptions(), Simple.class)
                                            .getWriteConcern());
    }

    @Test
    public void testFindAndDeleteWithCollation() {
        assumeMinServerVersion(3.4);
        getDs().getCollection(FacebookUser.class).drop();
        getDs().save(asList(new FacebookUser(1, "John Doe"),
                            new FacebookUser(2, "john doe")));

        Query<FacebookUser> query = getDs().find(FacebookUser.class)
                                           .field("username").equal("john doe");
        assertNotNull(getDs().findAndDelete(query));
        assertNull(getDs().findAndDelete(query));

        FindAndModifyOptions options = new FindAndModifyOptions()
            .collation(Collation.builder()
                                .locale("en")
                                .collationStrength(CollationStrength.SECONDARY)
                                .build());
        assertNotNull(getDs().findAndDelete(query, options));
        assertTrue("Options should not be modified by the datastore", options.isReturnNew());
        assertFalse("Options should not be modified by the datastore", options.isRemove());
    }

    @Test
    public void testFindAndDeleteWithNoQueryMatch() {
        assertNull(getDs().findAndDelete(getDs()
                                             .find(FacebookUser.class)
                                             .field("username").equal("David S. Pumpkins")));
    }

    private void testFirstDatastore(final Datastore ds1) {
        final FacebookUser user = ds1.find(FacebookUser.class).filter("id", 1)
                                     .find(new FindOptions().limit(1))
                                     .next();
        Assert.assertNotNull(user);
        Assert.assertNotNull(ds1.find(FacebookUser.class).filter("id", 3)
                                .find(new FindOptions().limit(1))
                                .next());

        Assert.assertEquals("Should find 1 friend", 1, user.friends.size());
        Assert.assertEquals("Should find the right friend", 3, user.friends.get(0).id);

        Assert.assertNull(ds1.find(FacebookUser.class).filter("id", 2)
                             .find(new FindOptions().limit(1))
                             .tryNext());
        Assert.assertNull(ds1.find(FacebookUser.class).filter("id", 4)
                             .find(new FindOptions().limit(1))
                             .tryNext());
    }

    private void testSecondDatastore(final Datastore ds2) {
        Assert.assertNull(ds2.find(FacebookUser.class).filter("id", 1)
                             .find(new FindOptions().limit(1))
                             .tryNext());
        Assert.assertNull(ds2.find(FacebookUser.class).filter("id", 3)
                             .find(new FindOptions().limit(1))
                             .tryNext());

        final FacebookUser db2FoundUser = ds2.find(FacebookUser.class).filter("id", 2)
                                             .find(new FindOptions().limit(1))
                                             .next();
        Assert.assertNotNull(db2FoundUser);
        Assert.assertNotNull(ds2.find(FacebookUser.class).filter("id", 4)
                                .find(new FindOptions().limit(1))
                                .next());
        Assert.assertEquals("Should find 1 friend", 1, db2FoundUser.friends.size());
        Assert.assertEquals("Should find the right friend", 4, db2FoundUser.friends.get(0).id);
    }

    private void testStandardDatastore() {
        Assert.assertNull(getDs().find(FacebookUser.class).filter("id", 1)
                                 .find(new FindOptions().limit(1))
                                 .tryNext());
        Assert.assertNull(getDs().find(FacebookUser.class).filter("id", 2)
                                 .find(new FindOptions().limit(1))
                                 .tryNext());
        Assert.assertNull(getDs().find(FacebookUser.class).filter("id", 3)
                                 .find(new FindOptions().limit(1))
                                 .tryNext());
        Assert.assertNull(getDs().find(FacebookUser.class).filter("id", 4)
                                 .find(new FindOptions().limit(1))
                                 .tryNext());
    }

    @Entity("facebook_users")
    public static class FacebookUser {
        @Id
        private long id;
        private String username;
        private int loginCount;
        @Reference
        private final List<FacebookUser> friends = new ArrayList<FacebookUser>();

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

    @Entity(value = "facebook_users", noClassnameStored = true)
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
            if (!dbObj.containsField("_id")) {
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

        @PostLoad
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
