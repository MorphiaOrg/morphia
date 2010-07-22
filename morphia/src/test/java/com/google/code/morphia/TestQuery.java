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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.types.CodeWScope;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
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
import com.google.code.morphia.testmodel.Hotel;
import com.google.code.morphia.testmodel.Rectangle;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoInternalException;

/**
 *
 * @author Scott Hernandez
 */
public class TestQuery  extends TestBase {

    public static class Photo {
        @Id String id;
        List<String> keywords = Collections.singletonList("amazing");
    }


    public static class PhotoWithKeywords {
        @Id String id;
        @Embedded List<Keyword> keywords = Arrays.asList(new Keyword("california"), new Keyword("neveda"), new Keyword("arizona"));
        public PhotoWithKeywords() {}
        public PhotoWithKeywords(final String... words) {
            keywords = new ArrayList<Keyword>((int) (words.length*1.3));
            for(String word : words)
            {
                keywords.add(new Keyword(word));
            }
        }
    }

    @Embedded
    public static class Keyword {
        String keyword;
        int score = 12;
        protected Keyword() {}
        public Keyword(final String k) { keyword = k;}
    }

    public static class ContainsPhotoKey {
        @Id String id;
        Key<Photo> photo;
    }

    @Entity
    public static class ContainsPic {
        @Id String id;
        String name = "test";
        @Reference
		Pic pic;
        @Reference(lazy=true)
		Pic lazyPic;
    }
    @Entity
    public static class Pic {
        @Id String id;
        String name;
    }

    public static class ContainsRenamedFields{
        @Id String id;
        @Property("first_name")
        String firstName = "Scott";
        @Property("last_name")
        String lastName = "Hernandez";
    }

    @Test
    public void testRenamedFieldQuery() throws Exception {
        ds.save(new ContainsRenamedFields());

        ContainsRenamedFields ent = null;
        try {
            //validation will cause an exception
            ent = ds.find(ContainsRenamedFields.class).field("firstName").equal("Scott").get();
        } catch (Exception e) {
        } finally {
            assertNull(ent);
        }
        ent = ds.find(ContainsRenamedFields.class).field("first_name").equal("Scott").get();
        assertNotNull(ent);
    }


    @Test
    public void testItemInListQuery() throws Exception {
        ds.save(new Photo());
        Photo p = ds.find(Photo.class).field("keywords").hasThisOne("amazing").get();
        assertNotNull(p);
        p = ds.find(Photo.class, "keywords in", "foo").get();
        assertNull(p);

        ds.save(new PhotoWithKeywords());
        assertNotNull(ds.find(PhotoWithKeywords.class, "keywords in", new Keyword("california")).get());
        assertNull(ds.find(PhotoWithKeywords.class, "keywords in", new Keyword("not")).get());
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
        Photo p = new Photo();
        ContainsPhotoKey cpk = new ContainsPhotoKey();
        cpk.photo = ds.save(p);
        ds.save(cpk);

        assertNotNull(ds.find(ContainsPhotoKey.class, "photo", p).get());
        assertNotNull(ds.find(ContainsPhotoKey.class, "photo", cpk.photo).get());
        assertNull(ds.find(ContainsPhotoKey.class, "photo", 1).get());
    }

    @Test
    public void testQueryOverReference() throws Exception {

        ContainsPic cpk = new ContainsPic();
        Pic p = new Pic();
        ds.save(p);
		cpk.pic = p;
        
		ds.save(cpk);

        Query<ContainsPic> query = ds.createQuery(ContainsPic.class);

		assertEquals(1, query.field("pic").equal(p).asList().size());
    }
    @Test
    public void testQueryOverLazyReference() throws Exception {

        ContainsPic cpk = new ContainsPic();
        Pic p = new Pic();
        ds.save(p);
		cpk.lazyPic = p;

		ds.save(cpk);

        Query<ContainsPic> query = ds.createQuery(ContainsPic.class);
		assertEquals(1, query.field("lazyPic").equal(p).asList().size());
    }


    @Test
    public void testWhereCodeWScopeQuery() throws Exception {
        ds.save(new PhotoWithKeywords());
//        CodeWScope hasKeyword = new CodeWScope("for (kw in this.keywords) { if(kw.keyword == kwd) return true; } return false;", new BasicDBObject("kwd","california"));
        CodeWScope hasKeyword = new CodeWScope("this.keywords != null", new BasicDBObject());
        assertNotNull(ds.find(PhotoWithKeywords.class).where(hasKeyword).get());
    }

    @Test
    public void testWhereStringQuery() throws Exception {
        ds.save(new PhotoWithKeywords());
        CodeWScope hasKeyword = new CodeWScope("this.keywords != null", new BasicDBObject());
        assertNotNull(ds.find(PhotoWithKeywords.class).where(hasKeyword.getCode()).get());
    }

    @Test
    public void testWhereWithInvalidStringQuery() throws Exception {
        ds.save(new PhotoWithKeywords());
        CodeWScope hasKeyword = new CodeWScope("keywords != null", new BasicDBObject());
		try {
			// must fail
	        assertNotNull(ds.find(PhotoWithKeywords.class).where(hasKeyword.getCode()).get());
			Assert.fail("Invalid javascript magically isn't invalid anymore?");
		} catch (MongoInternalException e) {
			// fine
		}

    }
    
    @Test
    public void testRegexQuery() throws Exception {
        ds.save(new PhotoWithKeywords());
        assertNotNull(ds.find(PhotoWithKeywords.class).disableValidation().filter("keywords.keyword", Pattern.compile("california")).get());
        assertNull(ds.find(PhotoWithKeywords.class, "keywords.keyword",  Pattern.compile("blah")).get());
    }

    @Test
    public void testDeepQuery() throws Exception {
        ds.save(new PhotoWithKeywords());
        assertNotNull(ds.find(PhotoWithKeywords.class, "keywords.keyword", "california").get());
        assertNull(ds.find(PhotoWithKeywords.class, "keywords.keyword", "not").get());
    }

    @Test
    public void testIdOnlyQuery() throws Exception {
        PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
        ds.save(pwk);
        
        PhotoWithKeywords pwkLoaded = ds.find(PhotoWithKeywords.class, "keywords.keyword", "scott").retrievedFields(true, "_id").get();
        assertNotNull(pwkLoaded);
        Assert.assertFalse(pwkLoaded.keywords.contains("scott"));
        Assert.assertEquals(3, pwkLoaded.keywords.size());
        
        pwkLoaded = ds.find(PhotoWithKeywords.class, "keywords.keyword", "scott").retrievedFields(false, "keywords").get();
        assertNotNull(pwkLoaded);
        Assert.assertFalse(pwkLoaded.keywords.contains("scott"));
        Assert.assertEquals(3, pwkLoaded.keywords.size());
    }

    @Test @Ignore
    public void testIdFieldNameQuery() throws Exception {
        PhotoWithKeywords pwk = new PhotoWithKeywords("scott", "hernandez");
        ds.save(pwk);
                
        PhotoWithKeywords pwkLoaded = ds.find(PhotoWithKeywords.class, "id !=", "scott").get();
        assertNotNull(pwkLoaded);
    }

    @Test @Ignore
    public void testComplexIdQuery() throws Exception {
    	CustomId cId = new CustomId();
    	cId.id = new ObjectId();
    	cId.type = "banker";
    	
    	UsesCustomIdObject ucio = new UsesCustomIdObject();
    	ucio.id = cId;
    	ucio.text = "hllo";
    	this.ds.save(ucio);
                
    	UsesCustomIdObject ucioLoaded = ds.find(UsesCustomIdObject.class, "_id.type", "banker").get();
        assertNotNull(ucioLoaded);
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
        PhotoWithKeywords pwk1 = new PhotoWithKeywords();
        PhotoWithKeywords pwk2 = new PhotoWithKeywords("Scott","Joe","Sarah");

        ds.save(pwk1, pwk2);
        PhotoWithKeywords pwkScott = ds.find(PhotoWithKeywords.class).field("keywords").hasThisElement(new Keyword("Scott")).get();
        assertNotNull(pwkScott);
        //TODO add back when $and is done (> 1.5)
        //		PhotoWithKeywords pwkScottSarah= ds.find(PhotoWithKeywords.class).field("keywords").hasThisElement(new Keyword[] {new Keyword("Scott"), new Keyword("Joe")}).get();
        //		assertNotNull(pwkScottSarah);
        PhotoWithKeywords pwkBad = ds.find(PhotoWithKeywords.class).field("keywords").hasThisElement(new Keyword("Randy")).get();
        assertNull(pwkBad);

    }

    @Test
    public void testKeyList() throws Exception {
        Rectangle rect = new Rectangle(1000, 1);
        Key<Rectangle> rectKey = ds.save(rect);

        assertEquals(rectKey.getId(), rect.getId());

        FacebookUser fbUser1 = new FacebookUser(1, "scott");
        FacebookUser fbUser2 = new FacebookUser(2, "tom");
        FacebookUser fbUser3 = new FacebookUser(3, "oli");
        FacebookUser fbUser4 = new FacebookUser(4, "frank");
        Iterable<Key<FacebookUser>> fbKeys = ds.save(fbUser1, fbUser2, fbUser3, fbUser4);
        assertEquals(fbUser1.id, 1);

        List<Key<FacebookUser>> fbUserKeys = new ArrayList<Key<FacebookUser>>();
        for(Key<FacebookUser> key :fbKeys)
        {
            fbUserKeys.add(key);
        }

        assertEquals(fbUser1.id, fbUserKeys.get(0).getId());
        assertEquals(fbUser2.id, fbUserKeys.get(1).getId());
        assertEquals(fbUser3.id, fbUserKeys.get(2).getId());
        assertEquals(fbUser4.id, fbUserKeys.get(3).getId());

        KeysKeysKeys k1 = new KeysKeysKeys(rectKey, fbUserKeys);
        Key<KeysKeysKeys> k1Key = ds.save(k1);
        assertEquals(k1.id, k1Key.getId());

        KeysKeysKeys k1Loaded = ds.get(k1);
        for(Key<FacebookUser> key :k1Loaded.users)
        {
            assertNotNull(key.getId());
        }

        assertNotNull(k1Loaded.rect.getId());
    }

    @Test
    public void testKeyListLookups() throws Exception {
        FacebookUser fbUser1 = new FacebookUser(1, "scott");
        FacebookUser fbUser2 = new FacebookUser(2, "tom");
        FacebookUser fbUser3 = new FacebookUser(3, "oli");
        FacebookUser fbUser4 = new FacebookUser(4, "frank");
        Iterable<Key<FacebookUser>> fbKeys = ds.save(fbUser1, fbUser2, fbUser3, fbUser4);
        assertEquals(fbUser1.id, 1);

        List<Key<FacebookUser>> fbUserKeys = new ArrayList<Key<FacebookUser>>();
        for(Key<FacebookUser> key :fbKeys)
        {
            fbUserKeys.add(key);
        }

        assertEquals(fbUser1.id, fbUserKeys.get(0).getId());
        assertEquals(fbUser2.id, fbUserKeys.get(1).getId());
        assertEquals(fbUser3.id, fbUserKeys.get(2).getId());
        assertEquals(fbUser4.id, fbUserKeys.get(3).getId());

        KeysKeysKeys k1 = new KeysKeysKeys(null, fbUserKeys);
        Key<KeysKeysKeys> k1Key = ds.save(k1);
        assertEquals(k1.id, k1Key.getId());

        KeysKeysKeys k1Reloaded = ds.get(k1);
        KeysKeysKeys k1Loaded = ds.getByKey(KeysKeysKeys.class, k1Key);
        assertNotNull(k1Reloaded);
        assertNotNull(k1Loaded);
        for(Key<FacebookUser> key :k1Loaded.users)
        {
            assertNotNull(key.getId());
        }

        assertEquals(k1Loaded.users.size(), 4);

        List<FacebookUser> fbUsers = ds.getByKeys(FacebookUser.class, k1Loaded.users);
        assertEquals(fbUsers.size(), 4);
        for(FacebookUser fbUser : fbUsers) {
            assertNotNull(fbUser);
            assertNotNull(fbUser.id);
            assertNotNull(fbUser.username);
        }
    }

    @Test
    public void testGetByKeysHetro() throws Exception {
        FacebookUser fbU= new FacebookUser(1, "scott");
        Rectangle r = new Rectangle(1,1);
        Iterable<Key<Object>> keys = ds.save(fbU, r);
        List<Object> entities = ds.getByKeys(keys);
        assertNotNull(entities);
        assertEquals(2, entities.size());
        int userCount=0, rectCount=0;
        for (Object o: entities) {
            if (o instanceof Rectangle)
            {
                rectCount++;
            }
            else if(o instanceof FacebookUser)
            {
                userCount++;
            }
        }
        assertEquals(1, rectCount);
        assertEquals(1, userCount);
    }

    @Test
    public void testNonexistantGet() throws Exception {
        assertNull(ds.get(Hotel.class, -1));
    }

    @Test
    public void testNonexistantFindGet() throws Exception {
        assertNull(ds.find(Hotel.class,"_id", -1).get());
    }

    @Test
    public void testSimpleSort() throws Exception {
        Rectangle[] rects = {
                new Rectangle(1, 10),
                new Rectangle(3, 8),
                new Rectangle(6, 10),
                new Rectangle(10, 10),
                new Rectangle(10, 1),
        };
        for(Rectangle rect: rects)
        {
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
    public void testCompoudSort() throws Exception {
        Rectangle[] rects = {
                new Rectangle(1, 10),
                new Rectangle(3, 8),
                new Rectangle(6, 10),
                new Rectangle(10, 10),
                new Rectangle(10, 1),
        };
        for(Rectangle rect: rects)
        {
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
        Rectangle[] rects = {	new Rectangle(1, 10),
                new Rectangle(1, 10),
                new Rectangle(1, 10),
                new Rectangle(10, 10),
                new Rectangle(10, 10),
        };
        for(Rectangle rect: rects)
        {
            ds.save(rect);
        }

        Query<Rectangle> q1 = ds.find(Rectangle.class, "height", 1D);
        Query<Rectangle> q2 = ds.find(Rectangle.class, "height", 10D);
        Query<Rectangle> q3 = ds.find(Rectangle.class, "width", 10D);

        assertEquals(3, ds.getCount(q1));
        assertEquals(2, ds.getCount(q2));
        assertEquals(5, ds.getCount(q3));

    }

    @Test
    public void testDeleteQuery() throws Exception {
        Rectangle[] rects = {	new Rectangle(1, 10),
                new Rectangle(1, 10),
                new Rectangle(1, 10),
                new Rectangle(10, 10),
                new Rectangle(10, 10),
        };
        for(Rectangle rect: rects)
        {
            ds.save(rect);
        }

        assertEquals(5, ds.getCount(Rectangle.class));
        ds.delete(ds.find(Rectangle.class, "height", 1D));
        assertEquals(2, ds.getCount(Rectangle.class));
    }

    @Test
    public void testRangeQuery() throws Exception {
        Rectangle[] rects = {
                new Rectangle(1, 10),
                new Rectangle(4, 2),
                new Rectangle(6, 10),
                new Rectangle(8, 5),
                new Rectangle(10, 4),
        };
        for(Rectangle rect: rects)
        {
            ds.save(rect);
        }

        assertEquals(4, ds.getCount(ds.createQuery(Rectangle.class).filter("height >", 3)));
        assertEquals(3, ds.getCount(ds.createQuery(Rectangle.class).filter("height <", 7)));
    }

    @Test
    public void testComplexRangeQuery() throws Exception {
        Rectangle[] rects = {
                new Rectangle(1, 10),
                new Rectangle(4, 2),
                new Rectangle(6, 10),
                new Rectangle(8, 5),
                new Rectangle(10, 4),
        };
        for(Rectangle rect: rects)
        {
            ds.save(rect);
        }

        assertEquals(2, ds.getCount(ds.createQuery(Rectangle.class).filter("height >", 3).filter("height <", 8)));
        assertEquals(1, ds.getCount(ds.createQuery(Rectangle.class).filter("height >", 3).filter("height <", 8).filter("width", 10)));
    }

}
