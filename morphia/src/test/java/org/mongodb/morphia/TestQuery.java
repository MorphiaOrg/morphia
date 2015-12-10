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


import com.jayway.awaitility.Awaitility;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.MongoInternalException;
import org.bson.types.CodeWScope;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestDatastore.FacebookUser;
import org.mongodb.morphia.TestDatastore.KeysKeysKeys;
import org.mongodb.morphia.TestMapper.CustomId;
import org.mongodb.morphia.TestMapper.UsesCustomIdObject;
import org.mongodb.morphia.annotations.CappedAt;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryImpl;
import org.mongodb.morphia.query.ValidationException;
import org.mongodb.morphia.testmodel.Hotel;
import org.mongodb.morphia.testmodel.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mongodb.morphia.testutil.JSONMatcher.jsonEqual;


/**
 * @author Scott Hernandez
 */
public class TestQuery extends TestBase {

    @Test
    public void genericMultiKeyValueQueries() {
        getMorphia().map(GenericKeyValue.class);
        getDs().ensureIndexes(GenericKeyValue.class);
        final GenericKeyValue<String> value = new GenericKeyValue<String>();
        final List<Object> keys = Arrays.<Object>asList("key1", "key2");
        value.key = keys;
        getDs().save(value);

        final Query<GenericKeyValue> query = getDs().createQuery(GenericKeyValue.class).field("key").hasAnyOf(keys);
        Assert.assertTrue(query.toString().replaceAll("\\s", "").contains("{\"$in\":[\"key1\",\"key2\"]"));
        assertEquals(query.get().id, value.id);
    }

    @Test
    public void maxScan() {
        final Pic pic1 = new Pic("pic1");
        final Pic pic2 = new Pic("pic2");
        final Pic pic3 = new Pic("pic3");
        final Pic pic4 = new Pic("pic4");

        getDs().save(pic1, pic2, pic3, pic4);

        assertEquals(2, getDs().createQuery(Pic.class).maxScan(2).asList().size());
        assertEquals(4, getDs().createQuery(Pic.class).asList().size());
    }

    @Test
    public void multiKeyValueQueries() {
        getMorphia().map(KeyValue.class);
        getDs().ensureIndexes(KeyValue.class);
        final KeyValue value = new KeyValue();
        final List<Object> keys = Arrays.<Object>asList("key1", "key2");
        value.key = keys;
        getDs().save(value);

        final Query<KeyValue> query = getDs().createQuery(KeyValue.class).field("key").hasAnyOf(keys);
        Assert.assertTrue(query.toString().replaceAll("\\s", "").contains("{\"$in\":[\"key1\",\"key2\"]"));
        assertEquals(query.get().id, value.id);
    }

    @Test
    public void referenceKeys() {
        final ReferenceKey key1 = new ReferenceKey("key1");

        final Pic pic1 = new Pic("pic1");
        final Pic pic2 = new Pic("pic2");
        final Pic pic3 = new Pic("pic3");
        final Pic pic4 = new Pic("pic4");
        getDs().save(key1, pic1, pic2, pic3, pic4);

        final ReferenceKeyValue value = new ReferenceKeyValue();
        value.id = key1;

        final Key<ReferenceKeyValue> key = getDs().save(value);

        final ReferenceKeyValue byKey = getDs().getByKey(ReferenceKeyValue.class, key);
        assertEquals(value.id, byKey.id);
    }

    @Override
    @After
    public void tearDown() {
        turnOffProfilingAndDropProfileCollection();
        super.tearDown();
    }

    @Test
    public void testAliasedFieldSort() throws Exception {
        final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(3, 8), new Rectangle(6, 10), new Rectangle(10, 10),
            new Rectangle(10, 1)};
        for (final Rectangle rect : array) {
            getDs().save(rect);
        }

        Rectangle r1 = getDs().find(Rectangle.class).limit(1).order("w").get();
        assertNotNull(r1);
        assertEquals(1, r1.getWidth(), 0);

        r1 = getDs().find(Rectangle.class).limit(1).order("-w").get();
        assertNotNull(r1);
        assertEquals(10, r1.getWidth(), 0);
    }

    @Test
    public void testCaseVariants() {
        getDs().save(new Pic("pic1"), new Pic("pic2"), new Pic("pic3"), new Pic("pic4"));

        assertEquals(0, getDs().createQuery(Pic.class)
                            .field("name").contains("PIC")
                            .asList().size());
        assertEquals(4, getDs().createQuery(Pic.class)
                               .field("name").containsIgnoreCase("PIC")
                               .asList().size());

        assertEquals(0, getDs().createQuery(Pic.class)
                               .field("name").equal("PIC1")
                               .asList().size());
        assertEquals(1, getDs().createQuery(Pic.class)
                               .field("name").equalIgnoreCase("PIC1")
                               .asList().size());

        assertEquals(0, getDs().createQuery(Pic.class)
                               .field("name").endsWith("C1")
                               .asList().size());
        assertEquals(1, getDs().createQuery(Pic.class)
                               .field("name").endsWithIgnoreCase("C1")
                               .asList().size());

        assertEquals(0, getDs().createQuery(Pic.class)
                               .field("name").startsWith("PIC")
                               .asList().size());
        assertEquals(4, getDs().createQuery(Pic.class)
                               .field("name").startsWithIgnoreCase("PIC")
                               .asList().size());
    }

    @Test
    public void testCombinationQuery() throws Exception {
        final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(4, 2), new Rectangle(6, 10), new Rectangle(8, 5),
            new Rectangle(10, 4)};
        for (final Rectangle rect : array) {
            getDs().save(rect);
        }

        Query<Rectangle> q;

        q = getDs().createQuery(Rectangle.class);
        q.and(q.criteria("width").equal(10), q.criteria("height").equal(1));

        assertEquals(1, getDs().getCount(q));

        q = getDs().createQuery(Rectangle.class);
        q.or(q.criteria("width").equal(10), q.criteria("height").equal(10));

        assertEquals(3, getDs().getCount(q));

        q = getDs().createQuery(Rectangle.class);
        q.or(q.criteria("width").equal(10), q.and(q.criteria("width").equal(5), q.criteria("height").equal(8)));

        assertEquals(3, getDs().getCount(q));
    }

    @Test
    public void testComplexIdQuery() throws Exception {
        final CustomId cId = new CustomId();
        cId.setId(new ObjectId());
        cId.setType("banker");

        final UsesCustomIdObject object = new UsesCustomIdObject();
        object.setId(cId);
        object.setText("hllo");
        getDs().save(object);

        assertNotNull(getDs().find(UsesCustomIdObject.class, "_id.type", "banker").get());
    }

    @Test
    public void testComplexIdQueryWithRenamedField() throws Exception {
        final CustomId cId = new CustomId();
        cId.setId(new ObjectId());
        cId.setType("banker");

        final UsesCustomIdObject object = new UsesCustomIdObject();
        object.setId(cId);
        object.setText("hllo");
        getDs().save(object);

        assertNotNull(getDs().find(UsesCustomIdObject.class, "_id.t", "banker").get());
    }

    @Test
    public void testComplexRangeQuery() throws Exception {
        final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(4, 2), new Rectangle(6, 10), new Rectangle(8, 5),
            new Rectangle(10, 4)
        };
        for (final Rectangle rect : array) {
            getDs().save(rect);
        }

        assertEquals(2, getDs().getCount(getDs().createQuery(Rectangle.class).filter("height >", 3).filter("height <", 8)));
        assertEquals(1, getDs().getCount(getDs().createQuery(Rectangle.class)
                                                .filter("height >", 3)
                                                .filter("height <", 8)
                                                .filter("width", 10)));
    }

    @Test
    public void testCompoundSort() throws Exception {
        final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(3, 8), new Rectangle(6, 10), new Rectangle(10, 10),
            new Rectangle(10, 1)};
        for (final Rectangle rect : array) {
            getDs().save(rect);
        }

        Rectangle r1 = getDs().find(Rectangle.class).order("width,-height").get();
        assertNotNull(r1);
        assertEquals(1, r1.getWidth(), 0);
        assertEquals(10, r1.getHeight(), 0);

        r1 = getDs().find(Rectangle.class).order("-height, -width").get();
        assertNotNull(r1);
        assertEquals(10, r1.getWidth(), 0);
        assertEquals(10, r1.getHeight(), 0);
    }

    @Test
    public void testCorrectQueryForNotWithSizeEqIssue514() {
        // given
        Query<PhotoWithKeywords> query = getAds().createQuery(PhotoWithKeywords.class);

        // when
        query.field("keywords").not().sizeEq(3);

        // then
        assertThat(query.toString(), jsonEqual("{ keywords: { $not: { $size: 3 } } }"));
    }

    @Test
    public void testDBObjectOrQuery() throws Exception {
        final PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
        getDs().save(pwk);

        final List<DBObject> orList = new ArrayList<DBObject>();
        orList.add(new BasicDBObject("keywords.keyword", "scott"));
        orList.add(new BasicDBObject("keywords.keyword", "ralph"));
        final BasicDBObject orQuery = new BasicDBObject("$or", orList);

        Query<PhotoWithKeywords> q = getAds().createQuery(PhotoWithKeywords.class, orQuery);
        assertEquals(1, q.countAll());

        q = getAds().createQuery(PhotoWithKeywords.class).disableValidation().filter("$or", orList);
        assertEquals(1, q.countAll());
    }

    @Test
    public void testDeepQuery() throws Exception {
        getDs().save(new PhotoWithKeywords());
        assertNotNull(getDs().find(PhotoWithKeywords.class, "keywords.keyword", "california").get());
        assertNull(getDs().find(PhotoWithKeywords.class, "keywords.keyword", "not").get());
    }

    @Test
    public void testDeepQueryWithBadArgs() throws Exception {
        getDs().save(new PhotoWithKeywords());
        PhotoWithKeywords p = getDs().find(PhotoWithKeywords.class, "keywords.keyword", 1).get();
        assertNull(p);
        p = getDs().find(PhotoWithKeywords.class, "keywords.keyword", "california".getBytes()).get();
        assertNull(p);
        p = getDs().find(PhotoWithKeywords.class, "keywords.keyword", null).get();
        assertNull(p);
    }

    @Test
    public void testDeepQueryWithRenamedFields() throws Exception {
        getDs().save(new PhotoWithKeywords());
        assertNotNull(getDs().find(PhotoWithKeywords.class, "keywords.keyword", "california").get());
        assertNull(getDs().find(PhotoWithKeywords.class, "keywords.keyword", "not").get());
    }

    @Test
    public void testDeleteQuery() throws Exception {
        final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(1, 10), new Rectangle(1, 10), new Rectangle(10, 10),
            new Rectangle(10, 10)};
        for (final Rectangle rect : array) {
            getDs().save(rect);
        }

        assertEquals(5, getDs().getCount(Rectangle.class));
        getDs().delete(getDs().find(Rectangle.class, "height", 1D));
        assertEquals(2, getDs().getCount(Rectangle.class));
    }

    @Test
    public void testElemMatchQuery() throws Exception {
        final PhotoWithKeywords pwk1 = new PhotoWithKeywords();
        final PhotoWithKeywords pwk2 = new PhotoWithKeywords("Scott", "Joe", "Sarah");

        getDs().save(pwk1, pwk2);
        Query<PhotoWithKeywords> query = getDs().find(PhotoWithKeywords.class)
                                                .field("keywords")
                                                .hasThisElement(new Keyword("Scott"));
        final PhotoWithKeywords pwkScott = query.get();
        assertNotNull(pwkScott);
        // TODO add back when $and is done (> 1.5)  this needs multiple $elemMatch clauses
        //        query = getDs().find(PhotoWithKeywords.class)
        //                       .field("keywords")
        //                       .hasThisElement(new Keyword[]{new Keyword("Scott"), new Keyword("Joe")});
        //        System.out.println("************ query = " + query);
        //        PhotoWithKeywords pwkScottSarah = query.get();
        //        assertNotNull(pwkScottSarah);
        final PhotoWithKeywords pwkBad = getDs().find(PhotoWithKeywords.class).field("keywords").hasThisElement(new Keyword("Randy")).get();
        assertNull(pwkBad);

    }

    @Test
    public void testExplainPlanIsReturnedAndContainsCorrectValueForN() {
        checkMinServerVersion(2.7);
        // Given
        getDs().save(new Pic("pic1"), new Pic("pic2"), new Pic("pic3"), new Pic("pic4"));

        // When
        Map<String, Object> explainResult = getDs().createQuery(Pic.class).explain();

        // Then
        assertEquals(4, ((Map) explainResult.get("executionStats")).get("nReturned"));
    }

    @Test
    public void testExplainPlanIsReturnedAndContainsCorrectValueForNForServersPriorTo27() {
        checkMaxServerVersion(2.7);
        // Given
        getDs().save(new Pic("pic1"), new Pic("pic2"), new Pic("pic3"), new Pic("pic4"));

        // When
        Map<String, Object> explainResult = getDs().createQuery(Pic.class).explain();

        // Then
        assertEquals(4, explainResult.get("n"));
    }

    @Test
    public void testFluentAndOrQuery() throws Exception {
        final PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
        getDs().save(pwk);

        final Query<PhotoWithKeywords> q = getAds().createQuery(PhotoWithKeywords.class);
        q.and(q.or(q.criteria("keywords.keyword").equal("scott")), q.or(q.criteria("keywords.keyword").equal("hernandez")));

        assertEquals(1, q.countAll());
        final QueryImpl<PhotoWithKeywords> qi = (QueryImpl<PhotoWithKeywords>) q;
        final DBObject realCriteria = qi.prepareCursor().getQuery();
        assertTrue(realCriteria.containsField("$and"));

    }

    @Test
    public void testFluentAndQuery1() throws Exception {
        final PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
        getDs().save(pwk);

        final Query<PhotoWithKeywords> q = getAds().createQuery(PhotoWithKeywords.class);
        q.and(q.criteria("keywords.keyword").hasThisOne("scott"),
              q.criteria("keywords.keyword").hasAnyOf(Arrays.asList("scott", "hernandez")));

        assertEquals(1, q.countAll());
        final QueryImpl<PhotoWithKeywords> qi = (QueryImpl<PhotoWithKeywords>) q;
        final DBObject realCriteria = qi.prepareCursor().getQuery();
        assertTrue(realCriteria.containsField("$and"));

    }

    @Test
    public void testFluentNotQuery() throws Exception {
        final PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
        getDs().save(pwk);

        final Query<PhotoWithKeywords> query = getAds().createQuery(PhotoWithKeywords.class);
        query.criteria("keywords.keyword").not().startsWith("ralph");

        assertEquals(1, query.countAll());
    }

    @Test
    public void testFluentOrQuery() throws Exception {
        final PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
        getDs().save(pwk);

        final Query<PhotoWithKeywords> q = getAds().createQuery(PhotoWithKeywords.class);
        q.or(q.criteria("keywords.keyword").equal("scott"), q.criteria("keywords.keyword").equal("ralph"));

        assertEquals(1, q.countAll());
    }

    @Test
    public void testGetByKeysHetero() throws Exception {
        final FacebookUser fbU = new FacebookUser(1, "scott");
        final Rectangle r = new Rectangle(1, 1);
        final Iterable<Key<Object>> keys = getDs().save(fbU, r);
        final List<Object> entities = getDs().getByKeys(keys);
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
    public void testIdFieldNameQuery() throws Exception {
        final PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
        getDs().save(pwk);

        final PhotoWithKeywords pwkLoaded = getDs().find(PhotoWithKeywords.class, "id !=", "scott").get();
        assertNotNull(pwkLoaded);
    }

    @Test
    public void testIdRangeQuery() throws Exception {
        getDs().save(new HasIntId(1), new HasIntId(11), new HasIntId(12));
        assertEquals(2, getDs().find(HasIntId.class).filter("_id >", 5).filter("_id <", 20).countAll());
        assertEquals(1, getDs().find(HasIntId.class).field("_id").greaterThan(0).field("_id").lessThan(11).countAll());
    }

    @Test
    public void testInQuery() throws Exception {
        final Photo photo = new Photo();
        photo.keywords = new ArrayList<String>();
        photo.keywords.add("red");
        photo.keywords.add("blue");
        photo.keywords.add("green");
        getDs().save(photo);

        final Set<String> keywords = new HashSet<String>();
        keywords.add("red");
        keywords.add("yellow");
        final Photo photoFound = getDs().find(Photo.class).field("keywords").in(keywords).get();
        assertNotNull(photoFound);
    }

    @Test
    public void testInQueryWithObjects() throws Exception {
        getDs().save(new PhotoWithKeywords(), new PhotoWithKeywords("Scott", "Joe", "Sarah"));

        final Set<Keyword> keywords = new HashSet<Keyword>();
        keywords.add(new Keyword("Scott"));
        keywords.add(new Keyword("Randy"));
        final PhotoWithKeywords pwkFound = getDs().find(PhotoWithKeywords.class).field("keywords").in(keywords).get();
        assertNotNull(pwkFound);
    }

    @Test
    public void testKeyList() throws Exception {
        final Rectangle rect = new Rectangle(1000, 1);
        final Key<Rectangle> rectKey = getDs().save(rect);

        assertEquals(rectKey.getId(), rect.getId());

        final FacebookUser fbUser1 = new FacebookUser(1, "scott");
        final FacebookUser fbUser2 = new FacebookUser(2, "tom");
        final FacebookUser fbUser3 = new FacebookUser(3, "oli");
        final FacebookUser fbUser4 = new FacebookUser(4, "frank");
        final Iterable<Key<FacebookUser>> fbKeys = getDs().save(fbUser1, fbUser2, fbUser3, fbUser4);
        assertEquals(1, fbUser1.getId());

        final List<Key<FacebookUser>> fbUserKeys = new ArrayList<Key<FacebookUser>>();
        for (final Key<FacebookUser> key : fbKeys) {
            fbUserKeys.add(key);
        }

        assertEquals(fbUser1.getId(), fbUserKeys.get(0).getId());
        assertEquals(fbUser2.getId(), fbUserKeys.get(1).getId());
        assertEquals(fbUser3.getId(), fbUserKeys.get(2).getId());
        assertEquals(fbUser4.getId(), fbUserKeys.get(3).getId());

        final KeysKeysKeys k1 = new KeysKeysKeys(rectKey, fbUserKeys);
        final Key<KeysKeysKeys> k1Key = getDs().save(k1);
        assertEquals(k1.getId(), k1Key.getId());

        final KeysKeysKeys k1Loaded = getDs().get(k1);
        for (final Key<FacebookUser> key : k1Loaded.getUsers()) {
            assertNotNull(key.getId());
        }

        assertNotNull(k1Loaded.getRect().getId());
    }

    @Test
    public void testKeyListLookups() throws Exception {
        final FacebookUser fbUser1 = new FacebookUser(1, "scott");
        final FacebookUser fbUser2 = new FacebookUser(2, "tom");
        final FacebookUser fbUser3 = new FacebookUser(3, "oli");
        final FacebookUser fbUser4 = new FacebookUser(4, "frank");
        final Iterable<Key<FacebookUser>> fbKeys = getDs().save(fbUser1, fbUser2, fbUser3, fbUser4);
        assertEquals(1, fbUser1.getId());

        final List<Key<FacebookUser>> fbUserKeys = new ArrayList<Key<FacebookUser>>();
        for (final Key<FacebookUser> key : fbKeys) {
            fbUserKeys.add(key);
        }

        assertEquals(fbUser1.getId(), fbUserKeys.get(0).getId());
        assertEquals(fbUser2.getId(), fbUserKeys.get(1).getId());
        assertEquals(fbUser3.getId(), fbUserKeys.get(2).getId());
        assertEquals(fbUser4.getId(), fbUserKeys.get(3).getId());

        final KeysKeysKeys k1 = new KeysKeysKeys(null, fbUserKeys);
        final Key<KeysKeysKeys> k1Key = getDs().save(k1);
        assertEquals(k1.getId(), k1Key.getId());

        final KeysKeysKeys k1Reloaded = getDs().get(k1);
        final KeysKeysKeys k1Loaded = getDs().getByKey(KeysKeysKeys.class, k1Key);
        assertNotNull(k1Reloaded);
        assertNotNull(k1Loaded);
        for (final Key<FacebookUser> key : k1Loaded.getUsers()) {
            assertNotNull(key.getId());
        }

        assertEquals(4, k1Loaded.getUsers().size());

        final List<FacebookUser> fbUsers = getDs().getByKeys(FacebookUser.class, k1Loaded.getUsers());
        assertEquals(4, fbUsers.size());
        for (final FacebookUser fbUser : fbUsers) {
            assertNotNull(fbUser);
            assertNotNull(fbUser.getId());
            assertNotNull(fbUser.getUsername());
        }
    }

    @Test
    public void testNegativeBatchSize() throws Exception {
        getDs().delete(getDs().find(PhotoWithKeywords.class));
        getDs().save(new PhotoWithKeywords("scott", "hernandez"),
                     new PhotoWithKeywords("scott", "hernandez"),
                     new PhotoWithKeywords("scott", "hernandez"));
        getDs().save(new PhotoWithKeywords("1", "2"), new PhotoWithKeywords("3", "4"), new PhotoWithKeywords("5", "6"));
        final List<PhotoWithKeywords> list = getDs().find(PhotoWithKeywords.class)
                                                    .batchSize(-2)
                                                    .asList();
        assertEquals(2, list.size());
    }

    @Test
    public void testNonSnapshottedQuery() throws Exception {
        getDs().delete(getDs().find(PhotoWithKeywords.class));
        getDs().save(new PhotoWithKeywords("scott", "hernandez"),
                     new PhotoWithKeywords("scott", "hernandez"),
                     new PhotoWithKeywords("scott", "hernandez"));
        final Iterator<PhotoWithKeywords> it = getDs().find(PhotoWithKeywords.class).enableSnapshotMode().batchSize(2).iterator();
        getDs().save(new PhotoWithKeywords("1", "2"), new PhotoWithKeywords("3", "4"), new PhotoWithKeywords("5", "6"));

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
    public void testNonexistentFindGet() throws Exception {
        assertNull(getDs().find(Hotel.class, "_id", -1).get());
    }

    @Test
    public void testNonexistentGet() throws Exception {
        assertNull(getDs().get(Hotel.class, -1));
    }

    @Test
    public void testNotGeneratesCorrectQueryForGreaterThan() throws Exception {
        // given
        final Query<Keyword> query = getAds().createQuery(Keyword.class);

        // when
        query.criteria("score").not().greaterThan(7);

        // then
        assertThat(query.toString(), jsonEqual("{ score: { $not: { $gt: 7} } }"));
    }

    @Test
    public void testNotGeneratesCorrectQueryForRegex() throws Exception {
        // given
        final Query<PhotoWithKeywords> query = getAds().createQuery(PhotoWithKeywords.class);

        // when
        query.criteria("keywords.keyword").not().startsWith("ralph");

        // then
        assertThat(query.toString(), jsonEqual("{ keywords.keyword: { $not: { $regex: '^ralph'} } }"));
    }

    @Test
    public void testQBE() throws Exception {
        final CustomId cId = new CustomId();
        cId.setId(new ObjectId());
        cId.setType("banker");

        final UsesCustomIdObject object = new UsesCustomIdObject();
        object.setId(cId);
        object.setText("hllo");
        getDs().save(object);
        final UsesCustomIdObject loaded;

        // Add back if/when query by example for embedded fields is supported (require dotting each field).
        // CustomId exId = new CustomId();
        // exId.type = cId.type;
        // loaded = getDs().find(UsesCustomIdObject.class, "_id", exId).get();
        // assertNotNull(loaded);

        final UsesCustomIdObject ex = new UsesCustomIdObject();
        ex.setText(object.getText());
        loaded = getDs().queryByExample(ex).get();
        assertNotNull(loaded);
    }

    @Test
    public void testQueryCount() throws Exception {
        final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(1, 10), new Rectangle(1, 10), new Rectangle(10, 10),
            new Rectangle(10, 10)};
        for (final Rectangle rect : array) {
            getDs().save(rect);
        }

        assertEquals(3, getDs().getCount(getDs().find(Rectangle.class, "height", 1D)));
        assertEquals(2, getDs().getCount(getDs().find(Rectangle.class, "height", 10D)));
        assertEquals(5, getDs().getCount(getDs().find(Rectangle.class, "width", 10D)));

    }

    @Test
    public void testQueryOverLazyReference() throws Exception {

        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        getDs().save(p);
        cpk.lazyPic = p;

        getDs().save(cpk);

        final Query<ContainsPic> query = getDs().createQuery(ContainsPic.class);
        assertEquals(1, query.field("lazyPic").equal(p).asList().size());
    }

    @Test
    public void testQueryOverReference() throws Exception {

        final ContainsPic cpk = new ContainsPic();
        final Pic p = new Pic();
        getDs().save(p);
        cpk.pic = p;

        getDs().save(cpk);

        final Query<ContainsPic> query = getDs().createQuery(ContainsPic.class);

        assertEquals(1, query.field("pic").equal(p).asList().size());

        try {
            getDs().find(ContainsPic.class, "pic.name", "foo").get();
            assertNull("um, query validation should have thrown");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("Cannot use dot-"));
        }
    }

    @Test
    public void testRangeQuery() throws Exception {
        final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(4, 2), new Rectangle(6, 10), new Rectangle(8, 5),
            new Rectangle(10, 4)
        };
        for (final Rectangle rect : array) {
            getDs().save(rect);
        }

        assertEquals(4, getDs().getCount(getDs().createQuery(Rectangle.class).filter("height >", 3)));
        assertEquals(3, getDs().getCount(getDs().createQuery(Rectangle.class).filter("height >", 3).filter("height <", 10)));
        assertEquals(1, getDs().getCount(getDs().createQuery(Rectangle.class).filter("height >", 9).filter("width <", 5)));
        assertEquals(3, getDs().getCount(getDs().createQuery(Rectangle.class).filter("height <", 7)));
    }

    @Test(expected = ValidationException.class)
    public void testReferenceQuery() throws Exception {
        final Photo p = new Photo();
        final ContainsPhotoKey cpk = new ContainsPhotoKey();
        cpk.photo = getDs().save(p);
        getDs().save(cpk);

        assertNotNull(getDs().find(ContainsPhotoKey.class, "photo", p).get());
        assertNotNull(getDs().find(ContainsPhotoKey.class, "photo", cpk.photo).get());
        assertNull(getDs().find(ContainsPhotoKey.class, "photo", 1).get());

        getDs().find(ContainsPhotoKey.class, "photo.keywords", "foo").get();
    }

    @Test
    public void testRegexInsensitiveQuery() throws Exception {
        getDs().save(new PhotoWithKeywords());
        final Pattern p = Pattern.compile("(?i)caLifornia");
        assertNotNull(getDs().find(PhotoWithKeywords.class).disableValidation().filter("keywords.keyword", p).get());
        assertNull(getDs().find(PhotoWithKeywords.class, "keywords.keyword", Pattern.compile("blah")).get());
    }

    @Test
    public void testRegexQuery() throws Exception {
        getDs().save(new PhotoWithKeywords());
        assertNotNull(getDs().find(PhotoWithKeywords.class)
                             .disableValidation()
                             .filter("keywords.keyword", Pattern.compile("california"))
                             .get());
        assertNull(getDs().find(PhotoWithKeywords.class, "keywords.keyword", Pattern.compile("blah")).get());
    }

    @Test
    public void testRenamedFieldQuery() throws Exception {
        getDs().save(new ContainsRenamedFields("Scott", "Bakula"));

        assertNotNull(getDs().find(ContainsRenamedFields.class).field("firstName").equal("Scott").get());

        assertNotNull(getDs().find(ContainsRenamedFields.class).field("first_name").equal("Scott").get());
    }

    @Test
    public void testRetrievedFields() throws Exception {
        ContainsRenamedFields user = new ContainsRenamedFields("Frank", "Zappa");
        getDs().save(user);

        ContainsRenamedFields found = getDs().find(ContainsRenamedFields.class).retrievedFields(true, "first_name").get();
        assertNotNull(found.firstName);
        assertNull(found.lastName);

        found = getDs().find(ContainsRenamedFields.class).retrievedFields(true, "firstName").get();
        assertNotNull(found.firstName);
        assertNull(found.lastName);

        try {
            getDs()
                .find(ContainsRenamedFields.class)
                .retrievedFields(true, "bad field name").get();
            Assert.fail("Validation should have caught the bad field");
        } catch (ValidationException e) {
            // success!
        }

        Query<ContainsRenamedFields> query = getDs()
            .find(ContainsRenamedFields.class)
            .retrievedFields(true, "_id", "first_name");
        DBObject fields = query.getFieldsObject();
        assertNull(fields.get(Mapper.CLASS_NAME_FIELDNAME));

    }

    @Test
    public void testSettingACommentInsertsCommentIntoProfileCollectionWhenProfilingIsTurnedOn() {
        // given
        getDs().save(new Pic("pic1"), new Pic("pic2"), new Pic("pic3"), new Pic("pic4"));

        getDb().command(new BasicDBObject("profile", 2));
        String expectedComment = "test comment";

        // when
        getDs().createQuery(Pic.class).comment(expectedComment).asList();

        // then
        DBCollection profileCollection = getDb().getCollection("system.profile");
        assertNotEquals(0, profileCollection.count());
        DBObject profileRecord = profileCollection.findOne(new BasicDBObject("op", "query")
                                                               .append("ns", getDs().getCollection(Pic.class).getFullName()));
        final Object commentPre32 = ((DBObject) profileRecord.get("query")).get("$comment");
        final Object commentPost32 = ((DBObject) profileRecord.get("query")).get("comment");
        assertTrue(profileRecord.toString(), expectedComment.equals(commentPre32) || expectedComment.equals(commentPost32));

        // finally
        turnOffProfilingAndDropProfileCollection();
    }

    @Test
    public void testShouldReturnOnlyTheFieldThatWasInTheIndexUsedForTheFindWhenReturnKeyIsUsed() {
        // Given
        // put some documents into the collection
        getDs().save(new Pic("pic1"), new Pic("pic2"), new Pic("pic3"), new Pic("pic4"));
        //set an index on the field "name"
        getDs().ensureIndex(Pic.class, "name");

        // When
        // find a document by using a search on the field in the index
        Query<Pic> query = getDs().createQuery(Pic.class).returnKey().field("name").equal("pic2");

        // Then
        Pic foundItem = query.get();
        assertNotNull(foundItem);
        assertThat("Name should be populated", foundItem.getName(), is("pic2"));
        assertNull("ID should not be populated", foundItem.getId());
    }

    @Test
    public void testSimpleSort() throws Exception {
        final Rectangle[] array = {new Rectangle(1, 10), new Rectangle(3, 8), new Rectangle(6, 10), new Rectangle(10, 10),
            new Rectangle(10, 1)};
        for (final Rectangle rect : array) {
            getDs().save(rect);
        }

        Rectangle r1 = getDs().find(Rectangle.class).limit(1).order("width").get();
        assertNotNull(r1);
        assertEquals(1, r1.getWidth(), 0);

        r1 = getDs().find(Rectangle.class).limit(1).order("-width").get();
        assertNotNull(r1);
        assertEquals(10, r1.getWidth(), 0);
    }

    @Test
    public void testSizeEqQuery() {
        // given
        Query<PhotoWithKeywords> query = getAds().createQuery(PhotoWithKeywords.class);

        // when
        query.field("keywords").sizeEq(3);

        // then
        assertThat(query.toString(), jsonEqual("{ keywords: { $size: 3 } }"));
    }

    @Test
    public void testSnapshottedQuery() throws Exception {
        getDs().delete(getDs().find(PhotoWithKeywords.class));
        getDs().save(new PhotoWithKeywords("scott", "hernandez"),
                     new PhotoWithKeywords("scott", "hernandez"),
                     new PhotoWithKeywords("scott", "hernandez"));
        final Iterator<PhotoWithKeywords> it = getDs().find(PhotoWithKeywords.class, "keywords.keyword", "scott")
                                                      .enableSnapshotMode()
                                                      .batchSize(2)
                                                      .iterator();
        getDs().save(new PhotoWithKeywords("1", "2"), new PhotoWithKeywords("3", "4"), new PhotoWithKeywords("5", "6"));

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
    public void testStartsWithQuery() throws Exception {
        getDs().save(new Photo());
        Photo p = getDs().find(Photo.class).field("keywords").startsWith("amaz").get();
        assertNotNull(p);
        p = getDs().find(Photo.class).field("keywords").startsWith("notareal").get();
        assertNull(p);

    }

    @Test
    public void testTailableCursors() {
        getMorphia().map(CappedPic.class);
        getDs().ensureCaps();
        final Query<CappedPic> query = getDs().createQuery(CappedPic.class);
        final List<CappedPic> found = new ArrayList<CappedPic>();
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        assertEquals(0, query.countAll());

        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                getDs().save(new CappedPic(System.currentTimeMillis() + ""));
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        final Iterator<CappedPic> tail = query.tail();
        Awaitility
            .await()
            .pollDelay(1, TimeUnit.SECONDS)
            .atMost(30, TimeUnit.SECONDS)
            .until(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    found.add(tail.next());
                    return found.size() >= 10;
                }
            });
        executorService.shutdownNow();
        Assert.assertTrue(query.countAll() >= 10);
    }

    @Test
    public void testWhereCodeWScopeQuery() throws Exception {
        getDs().save(new PhotoWithKeywords());
        //        CodeWScope hasKeyword = new CodeWScope("for (kw in this.keywords) { if(kw.keyword == kwd) return true; } return false;
        // ", new BasicDBObject("kwd","california"));
        final CodeWScope hasKeyword = new CodeWScope("this.keywords != null", new BasicDBObject());
        assertNotNull(getDs().find(PhotoWithKeywords.class).where(hasKeyword).get());
    }

    @Test
    public void testWhereStringQuery() throws Exception {
        getDs().save(new PhotoWithKeywords());
        assertNotNull(getDs().find(PhotoWithKeywords.class).where("this.keywords != null").get());
    }

    @Test
    public void testWhereWithInvalidStringQuery() throws Exception {
        getDs().save(new PhotoWithKeywords());
        final CodeWScope hasKeyword = new CodeWScope("keywords != null", new BasicDBObject());
        try {
            // must fail
            assertNotNull(getDs().find(PhotoWithKeywords.class).where(hasKeyword.getCode()).get());
            Assert.fail("Invalid javascript magically isn't invalid anymore?");
        } catch (MongoInternalException e) {
            // fine
        } catch (MongoException e) {
            // fine
        }

    }

    private void turnOffProfilingAndDropProfileCollection() {
        getDb().command(new BasicDBObject("profile", 0));
        DBCollection profileCollection = getDb().getCollection("system.profile");
        profileCollection.drop();
    }

    @Entity
    public static class Photo {
        @Id
        private ObjectId id;
        private List<String> keywords = Collections.singletonList("amazing");
    }

    public static class PhotoWithKeywords {
        @Id
        private ObjectId id;
        @Embedded
        private List<Keyword> keywords = Arrays.asList(new Keyword("california"), new Keyword("nevada"), new Keyword("arizona"));

        public PhotoWithKeywords() {
        }

        public PhotoWithKeywords(final String... words) {
            keywords = new ArrayList<Keyword>(words.length);
            for (final String word : words) {
                keywords.add(new Keyword(word));
            }
        }
    }

    @Embedded(concreteClass = Keyword.class)
    public static class Keyword {
        private String keyword;
        private int score = 12;

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
        private String name = "test";
        @Reference
        private Pic pic;
        @Reference(lazy = true)
        private Pic lazyPic;

        public ObjectId getId() {
            return id;
        }

        public void setId(final ObjectId id) {
            this.id = id;
        }

        public Pic getLazyPic() {
            return lazyPic;
        }

        public void setLazyPic(final Pic lazyPic) {
            this.lazyPic = lazyPic;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Pic getPic() {
            return pic;
        }

        public void setPic(final Pic pic) {
            this.pic = pic;
        }
    }

    @Entity
    public static class Pic {
        @Id
        private ObjectId id;
        private String name;

        public Pic() {
        }

        public Pic(final String name) {
            this.name = name;
        }

        public ObjectId getId() {
            return id;
        }

        public void setId(final ObjectId id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Entity(value = "capped_pic", cap = @CappedAt(count = 1000))
    public static class CappedPic extends Pic {
        public CappedPic() {
        }

        public CappedPic(final String name) {
            super(name);
        }
    }

    @Entity(noClassnameStored = true)
    public static class ContainsRenamedFields {
        @Id
        private ObjectId id;
        @Property("first_name")
        private String firstName;
        @Property("last_name")
        private String lastName;

        public ContainsRenamedFields() {
        }

        public ContainsRenamedFields(final String firstName, final String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    @Entity
    static class KeyValue {
        @Id
        private ObjectId id;
        /**
         * The list of keys for this value.
         */
        @Indexed(unique = true)
        private List<Object> key;
        /**
         * The id of the value document
         */
        @Indexed
        private ObjectId value;
    }

    @Entity
    static class GenericKeyValue<T> {

        @Id
        private ObjectId id;

        @Indexed(unique = true)
        private List<Object> key;

        @Embedded
        private T value;
    }

    @Entity
    static class ReferenceKeyValue {
        @Id
        private ReferenceKey id;
        /**
         * The list of keys for this value.
         */
        @Indexed(unique = true)
        @Reference
        private List<Pic> key;
        /**
         * The id of the value document
         */
        @Indexed
        private ObjectId value;
    }

    static class ReferenceKey {
        @Id
        private ObjectId id;
        private String name;

        ReferenceKey() {
        }

        ReferenceKey(final String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ReferenceKey that = (ReferenceKey) o;

            if (id != null ? !id.equals(that.id) : that.id != null) {
                return false;
            }
            if (name != null ? !name.equals(that.name) : that.name != null) {
                return false;
            }

            return true;
        }
    }
}
