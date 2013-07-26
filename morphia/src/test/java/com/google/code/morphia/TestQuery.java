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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.bson.types.CodeWScope;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.TestDatastore.FacebookUser;
import com.google.code.morphia.TestDatastore.KeysKeysKeys;
import com.google.code.morphia.TestMapper.CustomId;
import com.google.code.morphia.TestMapper.UsesCustomIdObject;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryImpl;
import com.google.code.morphia.query.ValidationException;
import com.google.code.morphia.testmodel.Hotel;
import com.google.code.morphia.testmodel.Rectangle;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.MongoInternalException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Scott Hernandez
 */
public class TestQuery extends TestBase {

  @Entity
  public static class Photo {
    @Id
    ObjectId id;
    List<String> keywords = Collections.singletonList("amazing");
  }


  public static class PhotoWithKeywords {
    @Id
    ObjectId id;
    @Embedded
    List<Keyword> keywords = Arrays.asList(new Keyword("california"), new Keyword("nevada"), new Keyword("arizona"));

    public PhotoWithKeywords() {
    }

    public PhotoWithKeywords(final String... words) {
      keywords = new ArrayList<Keyword>((int) (words.length));
      for (final String word : words) {
        keywords.add(new Keyword(word));
      }
    }
  }

  @Embedded(concreteClass = Keyword.class)
  public static class Keyword {
    String keyword;
    int score = 12;

    protected Keyword() {
    }

    public Keyword(final String k) {
      keyword = k;
    }
  }

  public static class ContainsPhotoKey {
    @Id
    private ObjectId id;
    private Key<Photo> photo;
  }

  @Entity
  public static class HasIntId {
    @Id
    private int id;

    protected HasIntId() {
    }

    HasIntId(final int id) {
      this.id = id;
    }
  }

  @Entity
  public static class ContainsPic {
    @Id
    private ObjectId id;
    String name = "test";
    @Reference
    Pic pic;
    @Reference(lazy = true)
    private Pic lazyPic;
  }

  @Entity
  public static class Pic {
    @Id
    ObjectId id;
    String name;

    String getName() {
      return name;
    }

    ObjectId getId() {
      return id;
    }
  }

  public static class ContainsRenamedFields {
    @Id
    ObjectId id;
    @Property("first_name")
    String firstName = "Scott";
    @Property("last_name")
    String lastName = "Hernandez";
  }

  @Test
  public void testRenamedFieldQuery() throws Exception {
    ds.save(new ContainsRenamedFields());

    ContainsRenamedFields ent;

    ent = ds.find(ContainsRenamedFields.class).field("firstName").equal("Scott").get();
    assertNotNull(ent);

    ent = ds.find(ContainsRenamedFields.class).field("first_name").equal("Scott").get();
    assertNotNull(ent);
  }

  @Test
  public void testStartsWithQuery() throws Exception {
    ds.save(new Photo());
    Photo p = ds.find(Photo.class).field("keywords").startsWith("amaz").get();
    assertNotNull(p);
    p = ds.find(Photo.class).field("keywords").startsWith("notareal").get();
    assertNull(p);

  }

  @Test
  public void testReferenceQuery() throws Exception {
    final Photo p = new Photo();
    final ContainsPhotoKey cpk = new ContainsPhotoKey();
    cpk.photo = ds.save(p);
    ds.save(cpk);

    assertNotNull(ds.find(ContainsPhotoKey.class, "photo", p).get());
    assertNotNull(ds.find(ContainsPhotoKey.class, "photo", cpk.photo).get());
    assertNull(ds.find(ContainsPhotoKey.class, "photo", 1).get());

    try {
      ds.find(ContainsPhotoKey.class, "photo.keywords", "foo").get();
      assertNull("um, query validation should have thrown");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().contains("could not be found"));
    }
  }

  @Test
  public void testQueryOverReference() throws Exception {

    final ContainsPic cpk = new ContainsPic();
    final Pic p = new Pic();
    ds.save(p);
    cpk.pic = p;

    ds.save(cpk);

    final Query<ContainsPic> query = ds.createQuery(ContainsPic.class);

    assertEquals(1, query.field("pic").equal(p).asList().size());

    try {
      ds.find(ContainsPic.class, "pic.name", "foo").get();
      assertNull("um, query validation should have thrown");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().contains("Can not use dot-"));
    }
  }

  @Test
  public void testQueryOverLazyReference() throws Exception {

    final ContainsPic cpk = new ContainsPic();
    final Pic p = new Pic();
    ds.save(p);
    cpk.lazyPic = p;

    ds.save(cpk);

    final Query<ContainsPic> query = ds.createQuery(ContainsPic.class);
    assertEquals(1, query.field("lazyPic").equal(p).asList().size());
  }


  @Test
  public void testWhereCodeWScopeQuery() throws Exception {
    ds.save(new PhotoWithKeywords());
    //        CodeWScope hasKeyword = new CodeWScope("for (kw in this.keywords) { if(kw.keyword == kwd) return true; } return false;", new BasicDBObject("kwd","california"));
    final CodeWScope hasKeyword = new CodeWScope("this.keywords != null", new BasicDBObject());
    assertNotNull(ds.find(PhotoWithKeywords.class).where(hasKeyword).get());
  }

  @Test
  public void testWhereStringQuery() throws Exception {
    ds.save(new PhotoWithKeywords());
    assertNotNull(ds.find(PhotoWithKeywords.class).where("this.keywords != null").get());
  }

  @Test
  public void testWhereWithInvalidStringQuery() throws Exception {
    ds.save(new PhotoWithKeywords());
    final CodeWScope hasKeyword = new CodeWScope("keywords != null", new BasicDBObject());
    try {
      // must fail
      assertNotNull(ds.find(PhotoWithKeywords.class).where(hasKeyword.getCode()).get());
      Assert.fail("Invalid javascript magically isn't invalid anymore?");
    } catch (MongoInternalException e) {
    } catch (MongoException e) {
      // fine
    }

  }

  @Test
  public void testRegexQuery() throws Exception {
    ds.save(new PhotoWithKeywords());
    assertNotNull(ds.find(PhotoWithKeywords.class).disableValidation().filter("keywords.keyword", Pattern.compile("california")).get());
    assertNull(ds.find(PhotoWithKeywords.class, "keywords.keyword", Pattern.compile("blah")).get());
  }

  @Test
  public void testRegexInsensitiveQuery() throws Exception {
    ds.save(new PhotoWithKeywords());
    final Pattern p = Pattern.compile("(?i)caLifornia");
    assertNotNull(ds.find(PhotoWithKeywords.class).disableValidation().filter("keywords.keyword", p).get());
    assertNull(ds.find(PhotoWithKeywords.class, "keywords.keyword", Pattern.compile("blah")).get());
  }

  @Test
  public void testDeepQuery() throws Exception {
    ds.save(new PhotoWithKeywords());
    assertNotNull(ds.find(PhotoWithKeywords.class, "keywords.keyword", "california").get());
    assertNull(ds.find(PhotoWithKeywords.class, "keywords.keyword", "not").get());
  }

  @Test
  public void testDeepQueryWithRenamedFields() throws Exception {
    ds.save(new PhotoWithKeywords());
    assertNotNull(ds.find(PhotoWithKeywords.class, "keywords.keyword", "california").get());
    assertNull(ds.find(PhotoWithKeywords.class, "keywords.keyword", "not").get());
  }

  @Test
  public void testSnapshottedQuery() throws Exception {
    ds.delete(ds.find(PhotoWithKeywords.class));
    ds.save(new PhotoWithKeywords("scott", "hernandez"), new PhotoWithKeywords("scott", "hernandez"), new PhotoWithKeywords("scott",
      "hernandez"));
    final Iterator<PhotoWithKeywords> it = ds.find(PhotoWithKeywords.class, "keywords.keyword", "scott").enableSnapshotMode().batchSize(2)
      .iterator();
    ds.save(new PhotoWithKeywords("1", "2"), new PhotoWithKeywords("3", "4"), new PhotoWithKeywords("5", "6"));

    PhotoWithKeywords pwkLoaded;
    pwkLoaded = it.next();
    assertNotNull(pwkLoaded);
    pwkLoaded = it.next();
    assertNotNull(pwkLoaded);
    //okay, now we should getMore...
    assertTrue(it.hasNext());
    pwkLoaded = it.next();
    assertNotNull(pwkLoaded);
    assertTrue(!it.hasNext());
  }

  @Test
  public void testNonSnapshottedQuery() throws Exception {
    ds.delete(ds.find(PhotoWithKeywords.class));
    ds.save(new PhotoWithKeywords("scott", "hernandez"), new PhotoWithKeywords("scott", "hernandez"), new PhotoWithKeywords("scott",
      "hernandez"));
    final Iterator<PhotoWithKeywords> it = ds.find(PhotoWithKeywords.class).enableSnapshotMode().batchSize(2).iterator();
    ds.save(new PhotoWithKeywords("1", "2"), new PhotoWithKeywords("3", "4"), new PhotoWithKeywords("5", "6"));

    PhotoWithKeywords pwkLoaded;
    pwkLoaded = it.next();
    assertNotNull(pwkLoaded);
    pwkLoaded = it.next();
    assertNotNull(pwkLoaded);
    //okay, now we should getMore...
    assertTrue(it.hasNext());
    pwkLoaded = it.next();
    assertNotNull(pwkLoaded);
    assertTrue(it.hasNext());
    pwkLoaded = it.next();
    assertNotNull(pwkLoaded);
  }


  @Test
  public void testIdOnlyQuery() throws Exception {
    final PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
    ds.save(pwk);

    PhotoWithKeywords pwkLoaded = ds.find(PhotoWithKeywords.class, "keywords.keyword", "scott").retrievedFields(true, "_id").get();
    assertNotNull(pwkLoaded);
    Assert.assertFalse(pwkLoaded.keywords.contains("scott"));
    assertEquals(3, pwkLoaded.keywords.size());

    pwkLoaded = ds.find(PhotoWithKeywords.class, "keywords.keyword", "scott").retrievedFields(false, "keywords").get();
    assertNotNull(pwkLoaded);
    Assert.assertFalse(pwkLoaded.keywords.contains("scott"));
    assertEquals(3, pwkLoaded.keywords.size());
  }

  @Test
  public void testDBObjectOrQuery() throws Exception {
    final PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
    ds.save(pwk);

    final AdvancedDatastore ads = (AdvancedDatastore) ds;
    final List<DBObject> orList = new ArrayList<DBObject>();
    orList.add(new BasicDBObject("keywords.keyword", "scott"));
    orList.add(new BasicDBObject("keywords.keyword", "ralph"));
    final BasicDBObject orQuery = new BasicDBObject("$or", orList);

    Query<PhotoWithKeywords> q = ads.createQuery(PhotoWithKeywords.class, orQuery);
    assertEquals(1, q.countAll());

    q = ads.createQuery(PhotoWithKeywords.class).disableValidation().filter("$or", orList);
    assertEquals(1, q.countAll());
  }

  @Test
  public void testFluentOrQuery() throws Exception {
    final PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
    ds.save(pwk);

    final AdvancedDatastore ads = (AdvancedDatastore) ds;
    final Query<PhotoWithKeywords> q = ads.createQuery(PhotoWithKeywords.class);
    q.or(q.criteria("keywords.keyword").equal("scott"), q.criteria("keywords.keyword").equal("ralph"));

    assertEquals(1, q.countAll());
  }

  @Test
  public void testFluentAndOrQuery() throws Exception {
    final PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
    ds.save(pwk);

    final AdvancedDatastore ads = (AdvancedDatastore) ds;
    final Query<PhotoWithKeywords> q = ads.createQuery(PhotoWithKeywords.class);
    q.and(q.or(q.criteria("keywords.keyword").equal("scott")), q.or(q.criteria("keywords.keyword").equal("hernandez")));

    assertEquals(1, q.countAll());
    final QueryImpl<PhotoWithKeywords> qi = (QueryImpl<PhotoWithKeywords>) q;
    final DBObject realCriteria = qi.prepareCursor().getQuery();
    assertTrue(realCriteria.containsField("$and"));

  }

  @Test
  public void testFluentAndQuery1() throws Exception {
    final PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
    ds.save(pwk);

    final AdvancedDatastore ads = (AdvancedDatastore) ds;
    final Query<PhotoWithKeywords> q = ads.createQuery(PhotoWithKeywords.class);
    q.and(q.criteria("keywords.keyword").hasThisOne("scott"), q.criteria("keywords.keyword").hasAnyOf(Arrays.asList("scott", "hernandez")));

    assertEquals(1, q.countAll());
    final QueryImpl<PhotoWithKeywords> qi = (QueryImpl<PhotoWithKeywords>) q;
    final DBObject realCriteria = qi.prepareCursor().getQuery();
    assertTrue(realCriteria.containsField("$and"));

  }


  @Test
  public void testFluentNotQuery() throws Exception {
    final PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
    ds.save(pwk);

    final AdvancedDatastore ads = (AdvancedDatastore) ds;
    final Query<PhotoWithKeywords> q = ads.createQuery(PhotoWithKeywords.class);
    q.criteria("keywords.keyword").not().startsWith("ralph");

    assertEquals(1, q.countAll());
  }


  @Test
  public void testIdFieldNameQuery() throws Exception {
    final PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
    ds.save(pwk);

    final PhotoWithKeywords pwkLoaded = ds.find(PhotoWithKeywords.class, "id !=", "scott").get();
    assertNotNull(pwkLoaded);
  }

  @Test
  public void testComplexIdQuery() throws Exception {
    final CustomId cId = new CustomId();
    cId.id = new ObjectId();
    cId.type = "banker";

    final UsesCustomIdObject object = new UsesCustomIdObject();
    object.id = cId;
    object.text = "hllo";
    ds.save(object);

    assertNotNull(ds.find(UsesCustomIdObject.class, "_id.type", "banker").get());
  }

  @Test
  public void testComplexIdQueryWithRenamedField() throws Exception {
    final CustomId cId = new CustomId();
    cId.id = new ObjectId();
    cId.type = "banker";

    final UsesCustomIdObject object = new UsesCustomIdObject();
    object.id = cId;
    object.text = "hllo";
    ds.save(object);

    assertNotNull(ds.find(UsesCustomIdObject.class, "_id.t", "banker").get());
  }

  @Test
  public void testQBE() throws Exception {
    final CustomId cId = new CustomId();
    cId.id = new ObjectId();
    cId.type = "banker";

    final UsesCustomIdObject object = new UsesCustomIdObject();
    object.id = cId;
    object.text = "hllo";
    ds.save(object);
    final UsesCustomIdObject loaded;

    //		Add back if/when query by example for embedded fields is supported (require dotting each field).
    //    	CustomId exId = new CustomId();
    //    	exId.type = cId.type;
    //    	loaded = ds.find(UsesCustomIdObject.class, "_id", exId).get();
    //      assertNotNull(loaded);

    final UsesCustomIdObject ex = new UsesCustomIdObject();
    ex.text = object.text;
    loaded = ds.queryByExample(ex).get();
    assertNotNull(loaded);
  }

  @Test
  public void testDeepQueryWithBadArgs() throws Exception {
    ds.save(new PhotoWithKeywords());
    PhotoWithKeywords p = ds.find(PhotoWithKeywords.class, "keywords.keyword", 1).get();
    assertNull(p);
    p = ds.find(PhotoWithKeywords.class, "keywords.keyword", "california".getBytes()).get();
    assertNull(p);
    p = ds.find(PhotoWithKeywords.class, "keywords.keyword", null).get();
    assertNull(p);
  }


  @Test
  public void testElemMatchQuery() throws Exception {
    final PhotoWithKeywords pwk1 = new PhotoWithKeywords();
    final PhotoWithKeywords pwk2 = new PhotoWithKeywords("Scott", "Joe", "Sarah");

    ds.save(pwk1, pwk2);
    final PhotoWithKeywords pwkScott = ds.find(PhotoWithKeywords.class).field("keywords").hasThisElement(new Keyword("Scott")).get();
    assertNotNull(pwkScott);
    //TODO add back when $and is done (> 1.5)
    //		PhotoWithKeywords pwkScottSarah= ds.find(PhotoWithKeywords.class).field("keywords").hasThisElement(new Keyword[] {new Keyword("Scott"), new Keyword("Joe")}).get();
    //		assertNotNull(pwkScottSarah);
    final PhotoWithKeywords pwkBad = ds.find(PhotoWithKeywords.class).field("keywords").hasThisElement(new Keyword("Randy")).get();
    assertNull(pwkBad);

  }

  @Test
  public void testKeyList() throws Exception {
    final Rectangle rect = new Rectangle(1000, 1);
    final Key<Rectangle> rectKey = ds.save(rect);

    assertEquals(rectKey.getId(), rect.getId());

    final FacebookUser fbUser1 = new FacebookUser(1, "scott");
    final FacebookUser fbUser2 = new FacebookUser(2, "tom");
    final FacebookUser fbUser3 = new FacebookUser(3, "oli");
    final FacebookUser fbUser4 = new FacebookUser(4, "frank");
    final Iterable<Key<FacebookUser>> fbKeys = ds.save(fbUser1, fbUser2, fbUser3, fbUser4);
    assertEquals(1, fbUser1.id);

    final List<Key<FacebookUser>> fbUserKeys = new ArrayList<Key<FacebookUser>>();
    for (final Key<FacebookUser> key : fbKeys) {
      fbUserKeys.add(key);
    }

    assertEquals(fbUser1.id, fbUserKeys.get(0).getId());
    assertEquals(fbUser2.id, fbUserKeys.get(1).getId());
    assertEquals(fbUser3.id, fbUserKeys.get(2).getId());
    assertEquals(fbUser4.id, fbUserKeys.get(3).getId());

    final KeysKeysKeys k1 = new KeysKeysKeys(rectKey, fbUserKeys);
    final Key<KeysKeysKeys> k1Key = ds.save(k1);
    assertEquals(k1.id, k1Key.getId());

    final KeysKeysKeys k1Loaded = ds.get(k1);
    for (final Key<FacebookUser> key : k1Loaded.users) {
      assertNotNull(key.getId());
    }

    assertNotNull(k1Loaded.rect.getId());
  }

  @Test
  public void testKeyListLookups() throws Exception {
    final FacebookUser fbUser1 = new FacebookUser(1, "scott");
    final FacebookUser fbUser2 = new FacebookUser(2, "tom");
    final FacebookUser fbUser3 = new FacebookUser(3, "oli");
    final FacebookUser fbUser4 = new FacebookUser(4, "frank");
    final Iterable<Key<FacebookUser>> fbKeys = ds.save(fbUser1, fbUser2, fbUser3, fbUser4);
    assertEquals(1, fbUser1.id);

    final List<Key<FacebookUser>> fbUserKeys = new ArrayList<Key<FacebookUser>>();
    for (final Key<FacebookUser> key : fbKeys) {
      fbUserKeys.add(key);
    }

    assertEquals(fbUser1.id, fbUserKeys.get(0).getId());
    assertEquals(fbUser2.id, fbUserKeys.get(1).getId());
    assertEquals(fbUser3.id, fbUserKeys.get(2).getId());
    assertEquals(fbUser4.id, fbUserKeys.get(3).getId());

    final KeysKeysKeys k1 = new KeysKeysKeys(null, fbUserKeys);
    final Key<KeysKeysKeys> k1Key = ds.save(k1);
    assertEquals(k1.id, k1Key.getId());

    final KeysKeysKeys k1Reloaded = ds.get(k1);
    final KeysKeysKeys k1Loaded = ds.getByKey(KeysKeysKeys.class, k1Key);
    assertNotNull(k1Reloaded);
    assertNotNull(k1Loaded);
    for (final Key<FacebookUser> key : k1Loaded.users) {
      assertNotNull(key.getId());
    }

    assertEquals(4, k1Loaded.users.size());

    final List<FacebookUser> fbUsers = ds.getByKeys(FacebookUser.class, k1Loaded.users);
    assertEquals(4, fbUsers.size());
    for (final FacebookUser fbUser : fbUsers) {
      assertNotNull(fbUser);
      assertNotNull(fbUser.id);
      assertNotNull(fbUser.username);
    }
  }

  @Test
  public void testGetByKeysHetero() throws Exception {
    final FacebookUser fbU = new FacebookUser(1, "scott");
    final Rectangle r = new Rectangle(1, 1);
    final Iterable<Key<Object>> keys = ds.save(fbU, r);
    final List<Object> entities = ds.getByKeys(keys);
    assertNotNull(entities);
    assertEquals(2, entities.size());
    int userCount = 0;
    int rectCount = 0;
    for (final Object o : entities) {
      if (o instanceof Rectangle) {
        rectCount++;
      } else if (o instanceof FacebookUser) {
        userCount++;
      }
    }
    assertEquals(1, rectCount);
    assertEquals(1, userCount);
  }

  @Test
  public void testNonexistentGet() throws Exception {
    assertNull(ds.get(Hotel.class, -1));
  }

  @Test
  public void testNonexistentFindGet() throws Exception {
    assertNull(ds.find(Hotel.class, "_id", -1).get());
  }

  @Test
  public void testSimpleSort() throws Exception {
    final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(3, 8), new Rectangle(6, 10), new Rectangle(10, 10), new Rectangle(10,
      1),};
    for (final Rectangle rect : array) {
      ds.save(rect);
    }

    Rectangle r1 = ds.find(Rectangle.class).limit(1).order("width").get();
    assertNotNull(r1);
    assertEquals(1, r1.getWidth(), 0);

    r1 = ds.find(Rectangle.class).limit(1).order("-width").get();
    assertNotNull(r1);
    assertEquals(10, r1.getWidth(), 0);
  }

  @Test
  public void testAliasedFieldSort() throws Exception {
    final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(3, 8), new Rectangle(6, 10), new Rectangle(10, 10), new Rectangle(10,
      1),};
    for (final Rectangle rect : array) {
      ds.save(rect);
    }

    Rectangle r1 = ds.find(Rectangle.class).limit(1).order("w").get();
    assertNotNull(r1);
    assertEquals(1, r1.getWidth(), 0);

    r1 = ds.find(Rectangle.class).limit(1).order("-w").get();
    assertNotNull(r1);
    assertEquals(10, r1.getWidth(), 0);
  }

  @Test
  public void testCompoundSort() throws Exception {
    final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(3, 8), new Rectangle(6, 10), new Rectangle(10, 10), new Rectangle(10,
      1),};
    for (final Rectangle rect : array) {
      ds.save(rect);
    }

    Rectangle r1 = ds.find(Rectangle.class).order("width,-height").get();
    assertNotNull(r1);
    assertEquals(1, r1.getWidth(), 0);
    assertEquals(10, r1.getHeight(), 0);

    r1 = ds.find(Rectangle.class).order("-height, -width").get();
    assertNotNull(r1);
    assertEquals(10, r1.getWidth(), 0);
    assertEquals(10, r1.getHeight(), 0);
  }

  @Test
  public void testQueryCount() throws Exception {
    final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(1, 10), new Rectangle(1, 10), new Rectangle(10, 10), new Rectangle(10,
      10),};
    for (final Rectangle rect : array) {
      ds.save(rect);
    }

    final Query<Rectangle> q1 = ds.find(Rectangle.class, "height", 1D);
    final Query<Rectangle> q2 = ds.find(Rectangle.class, "height", 10D);
    final Query<Rectangle> q3 = ds.find(Rectangle.class, "width", 10D);

    assertEquals(3, ds.getCount(q1));
    assertEquals(2, ds.getCount(q2));
    assertEquals(5, ds.getCount(q3));

  }

  @Test
  public void testDeleteQuery() throws Exception {
    final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(1, 10), new Rectangle(1, 10), new Rectangle(10, 10), new Rectangle(10,
      10),};
    for (final Rectangle rect : array) {
      ds.save(rect);
    }

    assertEquals(5, ds.getCount(Rectangle.class));
    ds.delete(ds.find(Rectangle.class, "height", 1D));
    assertEquals(2, ds.getCount(Rectangle.class));
  }

  @Test
  public void testIdRangeQuery() throws Exception {
    ds.save(new HasIntId(1), new HasIntId(11), new HasIntId(12));
    assertEquals(2, ds.find(HasIntId.class).filter("_id >", 5).filter("_id <", 20).countAll());
    assertEquals(1, ds.find(HasIntId.class).field("_id").greaterThan(0).field("_id").lessThan(11).countAll());
  }


  @Test
  public void testRangeQuery() throws Exception {
    final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(4, 2), new Rectangle(6, 10), new Rectangle(8, 5), new Rectangle(10, 4),};
    for (final Rectangle rect : array) {
      ds.save(rect);
    }

    assertEquals(4, ds.getCount(ds.createQuery(Rectangle.class).filter("height >", 3)));
    assertEquals(3, ds.getCount(ds.createQuery(Rectangle.class).filter("height >", 3).filter("height <", 10)));
    assertEquals(1, ds.getCount(ds.createQuery(Rectangle.class).filter("height >", 9).filter("width <", 5)));
    assertEquals(3, ds.getCount(ds.createQuery(Rectangle.class).filter("height <", 7)));
  }

  @Test
  public void testComplexRangeQuery() throws Exception {
    final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(4, 2), new Rectangle(6, 10), new Rectangle(8, 5), new Rectangle(10, 4),};
    for (final Rectangle rect : array) {
      ds.save(rect);
    }

    assertEquals(2, ds.getCount(ds.createQuery(Rectangle.class).filter("height >", 3).filter("height <", 8)));
    assertEquals(1, ds.getCount(ds.createQuery(Rectangle.class).filter("height >", 3).filter("height <", 8).filter("width", 10)));
  }

  @Test
  public void testCombinationQuery() throws Exception {
    final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(4, 2), new Rectangle(6, 10), new Rectangle(8, 5), new Rectangle(10, 4),};
    for (final Rectangle rect : array) {
      ds.save(rect);
    }

    Query<Rectangle> q;

    q = ds.createQuery(Rectangle.class);
    q.and(q.criteria("width").equal(10), q.criteria("height").equal(1));

    assertEquals(1, ds.getCount(q));

    q = ds.createQuery(Rectangle.class);
    q.or(q.criteria("width").equal(10), q.criteria("height").equal(10));

    assertEquals(3, ds.getCount(q));

    q = ds.createQuery(Rectangle.class);
    q.or(q.criteria("width").equal(10), q.and(q.criteria("width").equal(5), q.criteria("height").equal(8)));

    assertEquals(3, ds.getCount(q));
  }

  @Test
  public void testInQuery() throws Exception {
    final Photo photo = new Photo();
    photo.keywords = new ArrayList<String>();
    photo.keywords.add("red");
    photo.keywords.add("blue");
    photo.keywords.add("green");
    ds.save(photo);

    final Set<String> keywords = new HashSet<String>();
    keywords.add("red");
    keywords.add("yellow");
    final Photo photoFound = ds.find(Photo.class).field("keywords").in(keywords).get();
    assertNotNull(photoFound);
  }

  @Test
  public void testInQueryWithObjects() throws Exception {
    ds.save(new PhotoWithKeywords(), new PhotoWithKeywords("Scott", "Joe", "Sarah"));

    final Set<Keyword> keywords = new HashSet<Keyword>();
    keywords.add(new Keyword("Scott"));
    keywords.add(new Keyword("Randy"));
    final PhotoWithKeywords pwkFound = ds.find(PhotoWithKeywords.class).field("keywords").in(keywords).get();
    assertNotNull(pwkFound);
  }
}
