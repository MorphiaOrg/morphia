/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.google.code.morphia;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Test;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.EntityListeners;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PostPersist;
import com.google.code.morphia.annotations.PreLoad;
import com.google.code.morphia.annotations.PrePersist;
import com.google.code.morphia.annotations.Transient;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.query.UpdateException;
import com.google.code.morphia.testmodel.Address;
import com.google.code.morphia.testmodel.Hotel;
import com.google.code.morphia.testmodel.Rectangle;
import com.google.code.morphia.testutil.AssertedFailure;
import com.mongodb.BasicDBObject;
import com.mongodb.DBDecoderFactory;
import com.mongodb.DBObject;
import com.mongodb.LazyDBDecoder;
import com.mongodb.LazyWriteableDBDecoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Scott Hernandez
 */
public class TestDatastore extends TestBase {

  @Entity("facebook_users")
  public static class FacebookUser {
    @Id
    long id;
    String username;

    public FacebookUser() {
    }

    public FacebookUser(final long id, final String name) {
      this();
      this.id = id;
      username = name;
    }
  }

  public static class LifecycleListener {
    static boolean prePersist;
    static boolean prePersistWithEntity;

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

  @EntityListeners(LifecycleListener.class)
  public static class LifecycleTestObj {
    @Id
    ObjectId id;
    @Transient
    boolean prePersist;
    @Transient
    boolean postPersist;
    @Transient
    boolean preLoad;
    @Transient
    boolean postLoad;
    @Transient
    boolean postLoadWithParam;
    boolean prePersistWithParamAndReturn;
    boolean prePersistWithParam;
    boolean postPersistWithParam;
    boolean preLoadWithParamAndReturn;
    boolean preLoadWithParam;

    @PrePersist
    void prePersist() {
      if (prePersist) {
        throw new RuntimeException("already called");
      }

      prePersist = true;
    }

    @PrePersist
    protected void prePersistWithParam(final DBObject dbObj) {
      if (prePersistWithParam) {
        throw new RuntimeException("already called");
      }
      prePersistWithParam = true;
    }

    @PrePersist
    public DBObject prePersistWithParamAndReturn(final DBObject dbObj) {
      if (prePersistWithParamAndReturn) {
        throw new RuntimeException("already called");
      }
      prePersistWithParamAndReturn = true;
      return null;
      //			DBObject retObj = new BasicDBObject((Map)dbObj);
      //			retObj.put("prePersistWithParamAndReturn", true);
      //			return retObj;
    }

    @PostPersist
    private void postPersistPersist() {
      if (postPersist) {
        throw new RuntimeException("already called");
      }
      postPersist = true;

    }

    @PostPersist
    void postPersistWithParam(final DBObject dbObj) {
      //			dbObj.put("postPersistWithParam", true);
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
      //			dbObj.put("postLoadWithParam", true);
    }
  }

  public static class KeysKeysKeys {
    @Id
    ObjectId id;
    List<Key<FacebookUser>> users;
    Key<Rectangle> rect;

    protected KeysKeysKeys() {
    }

    public KeysKeysKeys(final Key<Rectangle> rectKey, final List<Key<FacebookUser>> users) {
      rect = rectKey;
      this.users = users;
    }
  }

  @Test
  public void testMorphiaDS() throws Exception {
    //		Datastore ds = new Morphia().createDatastore(mongo);
    new Morphia().createDatastore(mongo, "test");
  }

  @Test
  public void testLifecycle() throws Exception {
    final DBDecoderFactory oldFactory = ads.setDecoderFact(LazyWriteableDBDecoder.FACTORY);

    //only replace if using lazy decoder
    if (!(oldFactory instanceof LazyDBDecoder)) {
      ads.setDecoderFact(oldFactory);
    }

    try {
      final LifecycleTestObj life1 = new LifecycleTestObj();
      ((DatastoreImpl) ds).getMapper().addMappedClass(LifecycleTestObj.class);
      ds.save(life1);
      assertTrue(life1.prePersist);
      assertTrue(life1.prePersistWithParam);
      assertTrue(life1.prePersistWithParamAndReturn);
      assertTrue(life1.postPersist);
      assertTrue(life1.postPersistWithParam);

      final LifecycleTestObj loaded = ds.get(life1);
      assertTrue(loaded.preLoad);
      assertTrue(loaded.preLoadWithParam);
      assertTrue(loaded.preLoadWithParamAndReturn);
      assertTrue(loaded.postLoad);
      assertTrue(loaded.postLoadWithParam);
    } finally {
      ads.setDecoderFact(oldFactory);
    }
  }

  @Test
  public void testLifecycleListeners() throws Exception {
    final DBDecoderFactory oldFactory = ads.setDecoderFact(LazyWriteableDBDecoder.FACTORY);

    //only replace if using lazy decoder
    if (!(oldFactory instanceof LazyDBDecoder)) {
      ads.setDecoderFact(oldFactory);
    }

    try {
      final LifecycleTestObj life1 = new LifecycleTestObj();
      ((DatastoreImpl) ds).getMapper().addMappedClass(LifecycleTestObj.class);
      ds.save(life1);
      assertTrue(LifecycleListener.prePersist);
      assertTrue(LifecycleListener.prePersistWithEntity);
    } finally {
      ads.setDecoderFact(oldFactory);
    }
  }

  @Test
  public void testCollectionNames() throws Exception {
    assertEquals("facebook_users", morphia.getMapper().getCollectionName(FacebookUser.class));
  }

  @Test
  public void testGet() throws Exception {
    morphia.map(FacebookUser.class);
    final List<FacebookUser> fbUsers = new ArrayList<FacebookUser>();
    fbUsers.add(new FacebookUser(1, "user 1"));
    fbUsers.add(new FacebookUser(2, "user 2"));
    fbUsers.add(new FacebookUser(3, "user 3"));
    fbUsers.add(new FacebookUser(4, "user 4"));


    ds.save(fbUsers);
    assertEquals(4, ds.getCount(FacebookUser.class));
    assertNotNull(ds.get(FacebookUser.class, 1));
    final List<Long> ids = new ArrayList<Long>(2);
    ids.add(1L);
    ids.add(2L);
    final List<FacebookUser> res = ds.get(FacebookUser.class, ids).asList();
    assertEquals(2, res.size());
    assertNotNull(res.get(0));
    assertNotNull(res.get(0).id);
    assertNotNull(res.get(1));
    assertNotNull(res.get(1).username);
  }

  @Test
  public void testExists() throws Exception {
    final Key<FacebookUser> k = ds.save(new FacebookUser(1, "user 1"));
    assertEquals(1, ds.getCount(FacebookUser.class));
    assertNotNull(ds.get(FacebookUser.class, 1));
    assertNotNull(ds.exists(k));
    assertNotNull(ds.getByKey(FacebookUser.class, k));
    ds.delete(ds.find(FacebookUser.class));
    assertEquals(0, ds.getCount(FacebookUser.class));
    assertNull(ds.exists(k));

  }

  @Test
  public void testExistsWithEntity() throws Exception {
    final FacebookUser facebookUser = new FacebookUser(1, "user one");
    ds.save(facebookUser);
    assertEquals(1, ds.getCount(FacebookUser.class));
    assertNotNull(ds.get(FacebookUser.class, 1));
    assertNotNull(ds.exists(facebookUser));
    ds.delete(ds.find(FacebookUser.class));
    assertEquals(0, ds.getCount(FacebookUser.class));
    assertNull(ds.exists(facebookUser));
  }

  public void testIdUpdatedOnSave() throws Exception {
    final Rectangle rect = new Rectangle(10, 10);
    ds.save(rect);
    assertNotNull(rect.getId());
  }

  @Test
  public void testSaveAndDelete() throws Exception {
    final Rectangle rect = new Rectangle(10, 10);
    rect.setId("1");


    //test delete(entity)
    ds.save(rect);
    assertEquals(1, ds.getCount(rect));
    ds.delete(rect);
    assertEquals(0, ds.getCount(rect));

    //test delete(entity, id)
    ds.save(rect);
    assertEquals(1, ds.getCount(rect));
    ds.delete(rect.getClass(), 1);
    assertEquals(1, ds.getCount(rect));
    ds.delete(rect.getClass(), "1");
    assertEquals(0, ds.getCount(rect));

    //test delete(entity, {id})
    ds.save(rect);
    assertEquals(1, ds.getCount(rect));
    final List<String> ids = new ArrayList<String>();
    ids.add("1");
    ds.delete(rect.getClass(), ids);
    assertEquals(0, ds.getCount(rect));

    //test delete(entity, {id,id})
    rect.setId("1");
    ds.save(rect);
    rect.setId("2");
    ds.save(rect);
    assertEquals(2, ds.getCount(rect));
    ids.clear();
    ids.add("1");
    ids.add("2");
    ds.delete(rect.getClass(), ids);
    assertEquals(0, ds.getCount(rect));

    //test delete(entity, {id,id}) with autogenerated ids
    ids.clear();
    rect.setId(new ObjectId().toString()); // rect1
    ds.save(rect);
    ids.add(rect.getId());
    rect.setId(new ObjectId().toString()); // rect2
    ds.save(rect);
    ids.add(rect.getId());
    assertEquals("datastore should have saved two entities with autogenerated ids", 2, ds.getCount(rect));
    ds.delete(rect.getClass(), ids);
    assertEquals("datastore should have deleted two entities with autogenerated ids", 0, ds.getCount(rect));

    //test delete(entity, {id}) with one left
    rect.setId("1");
    ds.save(rect);
    rect.setId("2");
    ds.save(rect);
    assertEquals(2, ds.getCount(rect));
    ids.clear();
    ids.add("1");
    ds.delete(rect.getClass(), ids);
    assertEquals(1, ds.getCount(rect));

    //test delete(Class, {id}) with one left
    rect.setId("1");
    ds.save(rect);
    rect.setId("2");
    ds.save(rect);
    assertEquals(2, ds.getCount(rect));
    ids.clear();
    ids.add("1");
    ds.delete(Rectangle.class, ids);
    assertEquals(1, ds.getCount(rect));
  }

  @Test
  public void testEmbedded() throws Exception {
    ds.delete(ds.createQuery(Hotel.class));
    final Hotel borg = Hotel.create();
    borg.setName("Hotel Borg");
    borg.setStars(4);
    borg.setTakesCreditCards(true);
    borg.setStartDate(new Date());
    borg.setType(Hotel.Type.LEISURE);
    final Address address = new Address();
    address.setStreet("Posthusstraeti 11");
    address.setPostCode("101");
    borg.setAddress(address);


    ds.save(borg);
    assertEquals(1, ds.getCount(Hotel.class));
    assertNotNull(borg.getId());

    final Hotel hotelLoaded = ds.get(Hotel.class, borg.getId());
    assertEquals(borg.getName(), hotelLoaded.getName());
    assertEquals(borg.getAddress().getPostCode(), hotelLoaded.getAddress().getPostCode());
  }

  @Test
  public void testAuthentication() throws Exception {
    new AssertedFailure(AuthenticationException.class) {

      @Override
      protected void thisMustFail() {
        morphia.createDatastore(mongo, db.getName(), "SomeWeirdUserName" + System.nanoTime(),
            ("SomeWeirdPassword" + System.nanoTime()).toCharArray());
      }
    };
  }

  @Test(expected = UpdateException.class)
  public void saveNull() {
    ds.save((Hotel)null);
  }
}
