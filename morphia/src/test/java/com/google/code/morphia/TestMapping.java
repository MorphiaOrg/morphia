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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;

import org.junit.Test;

import com.google.code.morphia.annotations.CollectionName;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.testmodel.Address;
import com.google.code.morphia.testmodel.Article;
import com.google.code.morphia.testmodel.Circle;
import com.google.code.morphia.testmodel.Hotel;
import com.google.code.morphia.testmodel.PhoneNumber;
import com.google.code.morphia.testmodel.Rectangle;
import com.google.code.morphia.testmodel.RecursiveChild;
import com.google.code.morphia.testmodel.RecursiveParent;
import com.google.code.morphia.testmodel.Translation;
import com.google.code.morphia.testmodel.TravelAgency;
import com.google.code.morphia.utils.Key;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.Mongo;

/**
 *
 *
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class TestMapping {

	@Entity
	public static class MissingId {
		String id;
	}
	
	public static class MissingIdStill {
		String id;
	}
	
	@Entity("no-id")
	public static class MissingIdRenamed {
		String id;
	}
	
	@Embedded
	public static class IdOnEmbedded {
		@Id String id;
	}

	@Embedded
	public static class CollectionNameOnEmbedded {
		@CollectionName String collName;
	}
	
	@Embedded("no-id")
	public static class RenamedEmbedded {
		String name;
	}
	
	public static class IPrintAWarning {
		@Id String id;
		NotEmbeddable ne= new NotEmbeddable();
		String sValue = "not empty";
	}
	
	public static class NotEmbeddable {
		String noImNot;
	}

	public static class ContainsRef {
		public @Id String id;
		public DBRef rect;
	}
	
	public static class ContainsbyteArray {
		@Id String id;
		byte[] bytes = "Scott".getBytes();
	}

	public static class ContainsLongAndStringArray {
		@Id String id;
		private Long[] longs = {0L, 1L, 2L};
		String[] strings = {"Scott", "Rocks"};
	}
	
	public static class ContainsCollection {
		@Id String id;
		Collection<String> coll = new ArrayList<String>();
		
		private ContainsCollection() {
			coll.add("hi"); coll.add("Scott");
		}
	}

	@Test
    public void testCollectionMapping() throws Exception {
		DatastoreService.setDatabase("morphia_test");
		Datastore ds = (Datastore)DatastoreService.getDatastore();
		DatastoreService.mapClass(ContainsCollection.class);
		Key<ContainsCollection> savedKey = ds.save(new ContainsCollection());
		ContainsCollection loaded = ds.get(ContainsCollection.class, savedKey.getId());
		assertEquals(loaded.coll, (new ContainsCollection()).coll);
		assertNotNull(loaded.id);        
	}
	
	@Test
    public void testbyteArrayMapping() throws Exception {
		DatastoreService.setDatabase("morphia_test");
		Datastore ds = (Datastore)DatastoreService.getDatastore();
		DatastoreService.mapClass(ContainsbyteArray.class);
		Key<ContainsbyteArray> savedKey = ds.save(new ContainsbyteArray());
		ContainsbyteArray loaded = ds.get(ContainsbyteArray.class, savedKey.getId());
		assertEquals(new String(loaded.bytes), new String((new ContainsbyteArray()).bytes));
		assertNotNull(loaded.id);        
	}

	@SuppressWarnings("deprecation")
	@Test
    public void testLongArrayMapping() throws Exception {
		DatastoreService.setDatabase("morphia_test");
		Datastore ds = (Datastore)DatastoreService.getDatastore();
		DatastoreService.mapClass(ContainsLongAndStringArray.class);
		ds.save(new ContainsLongAndStringArray());
		ContainsLongAndStringArray loaded = ds.<ContainsLongAndStringArray>find(ContainsLongAndStringArray.class).get();
		assertEquals(loaded.longs, (new ContainsLongAndStringArray()).longs);
		assertEquals(loaded.strings, (new ContainsLongAndStringArray()).strings);
		assertNotNull(loaded.id);        
	}

	@Test
    public void testDbRefMapping() throws Exception {
        Morphia morphia = new Morphia();
        Mongo mongo = new Mongo();
        DB db = mongo.getDB("morphia_test");
        
        morphia.map(ContainsRef.class).map(Rectangle.class);
        DBCollection stuff = db.getCollection("stuff");
        DBCollection rectangles = db.getCollection("rectangles");
        
        assertTrue("'ne' field should not be persisted!",
        		!morphia.getMappedClasses().get(ContainsRef.class.getName()).containsFieldName("ne"));

        Rectangle r = new Rectangle(1,1);
        DBObject rDbObject = morphia.toDBObject(r);
        rDbObject.put("_ns", rectangles.getName());
        rectangles.save(rDbObject);
        
        ContainsRef cRef = new ContainsRef();
        cRef.rect = new DBRef(null, (String)rDbObject.get("_ns"), rDbObject.get("_id"));
        DBObject cRefDbOject = morphia.toDBObject(cRef);
        stuff.save(cRefDbOject);
        BasicDBObject cRefDbObjectLoaded =(BasicDBObject)stuff.findOne(BasicDBObjectBuilder.start("_id", cRefDbOject.get("_id")).get());
        ContainsRef cRefLoaded = morphia.fromDBObject(ContainsRef.class, cRefDbObjectLoaded);
        assertNotNull(cRefLoaded);
        assertNotNull(cRefLoaded.rect);
        assertNotNull(cRefLoaded.rect.getId());
        assertNotNull(cRefLoaded.rect.getRef());
        assertEquals(cRefLoaded.rect.getId(), cRef.rect.getId());
        assertEquals(cRefLoaded.rect.getRef(), cRef.rect.getRef());    
	}
	
	
	@Test
    public void testBadMappings() throws Exception {
        Morphia morphia = new Morphia();
        morphia.map(IPrintAWarning.class);
        
        assertTrue("'ne' field should not be persisted!",
        		!morphia.getMappedClasses().get(IPrintAWarning.class.getName()).containsFieldName("ne"));
        
        boolean allGood=false;
        try {
        	morphia.map(MissingId.class);
        } catch (MongoMappingException e) {
        	allGood = true;
        }
        assertTrue("Validation: Missing @Id field not caught", allGood);

        allGood = false;
        try {
        	morphia.map(IdOnEmbedded.class);
        } catch (MongoMappingException e) {
        	allGood = true;
        }
        assertTrue("Validation: @Id field on @Embedded not caught", allGood);

        allGood = false;
        try {
        	morphia.map(RenamedEmbedded.class);
        } catch (MongoMappingException e) {
        	allGood = true;
        }
        assertTrue("Validation: @Embedded(\"name\") not caught on Class", allGood);

        allGood = false;
        try {
        	morphia.map(MissingIdStill.class);
        } catch (MongoMappingException e) {
        	allGood = true;
        }
        assertTrue("Validation: Missing @Id field not not caught", allGood);

        allGood = false;
        try {
        	morphia.map(MissingIdRenamed.class);
        } catch (MongoMappingException e) {
        	allGood = true;
        }
        assertTrue("Validation: Missing @Id field not not caught", allGood);
    }
    
    
    @Test
    public void testBasicMapping() throws Exception {
        Mongo mongo = new Mongo();
        DB db = mongo.getDB("morphia_test");
        try {

            DBCollection hotels = db.getCollection("hotels");
            DBCollection agencies = db.getCollection("agencies");

            Morphia morphia = new Morphia();
            morphia.map(Hotel.class);
            morphia.map(TravelAgency.class);

            Hotel borg = Hotel.create();
            borg.setName("Hotel Borg");
            borg.setStars(4);
            borg.setTakesCreditCards(true);
            borg.setStartDate(new Date());
            borg.setType(Hotel.Type.LEISURE);
            borg.getTags().add("Swimming pool");
            borg.getTags().add("Room service");
            borg.setTemp("A temporary transient value");
            borg.getPhoneNumbers().add(new PhoneNumber(354,5152233,PhoneNumber.Type.PHONE));
            borg.getPhoneNumbers().add(new PhoneNumber(354,5152244,PhoneNumber.Type.FAX));

            Address borgAddr = new Address();
            borgAddr.setStreet("Posthusstraeti 11");
            borgAddr.setPostCode("101");
            borg.setAddress(borgAddr);
            borg.setCollectionName(hotels.getName());
            
            BasicDBObject hotelDbObj = (BasicDBObject) morphia.toDBObject(borg);
            hotels.save(hotelDbObj);

            Hotel borgLoaded = morphia.fromDBObject(Hotel.class, hotelDbObj);

            assertEquals(borg.getName(), borgLoaded.getName());
            assertEquals(borg.getStars(), borgLoaded.getStars());
            assertEquals(borg.getStartDate(), borgLoaded.getStartDate());
            assertEquals(borg.getType(), borgLoaded.getType());
            assertEquals(borg.getAddress().getStreet(), borgLoaded.getAddress().getStreet());
            assertEquals(borg.getTags().size(), borgLoaded.getTags().size());
            assertEquals(borg.getTags(), borgLoaded.getTags());
            assertEquals(borg.getPhoneNumbers().size(), borgLoaded.getPhoneNumbers().size());
            assertEquals(borg.getPhoneNumbers().get(1), borgLoaded.getPhoneNumbers().get(1));
            assertNull(borgLoaded.getTemp());
            assertTrue(borgLoaded.getPhoneNumbers() instanceof Vector);
            assertNotNull(borgLoaded.getId());

            TravelAgency agency = new TravelAgency();
            agency.setName("Lastminute.com");
            agency.getHotels().add(borgLoaded);
            agency.setCollectionName(agencies.getName());
            
            BasicDBObject agencyDbObj = (BasicDBObject) morphia.toDBObject(agency);
            agencies.save(agencyDbObj);

            TravelAgency agencyLoaded = morphia.fromDBObject(TravelAgency.class, (BasicDBObject)agencies.findOne(new BasicDBObject(Mapper.ID_KEY, agencyDbObj.get(Mapper.ID_KEY))));

            assertEquals(agency.getName(), agencyLoaded.getName());
            assertEquals(agency.getHotels().size(), 1);
            assertEquals(agency.getHotels().get(0).getName(), borg.getName());

        } finally {
            db.dropDatabase();
        }
    }

    @Test
    public void testMaps() throws Exception {
        Mongo mongo = new Mongo();
        DB db = mongo.getDB("morphia_test");
        try {
            DBCollection articles = db.getCollection("articles");
            Morphia morphia = new Morphia();
            morphia.map(Article.class).map(Translation.class).map(Circle.class);

            Article related = new Article();
            related.setCollectionName(articles.getName());
            BasicDBObject relatedDbObj = (BasicDBObject) morphia.toDBObject(related);
            articles.save(relatedDbObj);

            Article relatedLoaded = morphia.fromDBObject(Article.class, (BasicDBObject)articles.findOne(new BasicDBObject(Mapper.ID_KEY, relatedDbObj.get(Mapper.ID_KEY))));

            Article article = new Article();
            article.setTranslation("en", new Translation("Hello World", "Just a test"));
            article.setTranslation("is", new Translation("Halló heimur", "Bara að prófa"));

            article.setAttribute("myDate", new Date());
            article.setAttribute("myString", "Test");
            article.setAttribute("myInt", 123);

            article.putRelated("test", relatedLoaded);

            article.setCollectionName(articles.getName());
            BasicDBObject articleDbObj = (BasicDBObject) morphia.toDBObject(article);
            articles.save(articleDbObj);

            Article articleLoaded = morphia.fromDBObject(Article.class, (BasicDBObject)articles.findOne(new BasicDBObject(Mapper.ID_KEY, articleDbObj.get(Mapper.ID_KEY))));

            assertEquals(article.getTranslations().size(), articleLoaded.getTranslations().size());
            assertEquals(article.getTranslation("en").getTitle(), articleLoaded.getTranslation("en").getTitle());
            assertEquals(article.getTranslation("is").getBody(), articleLoaded.getTranslation("is").getBody());
            assertEquals(article.getAttributes().size(), articleLoaded.getAttributes().size());
            assertEquals(article.getAttribute("myDate"), articleLoaded.getAttribute("myDate"));
            assertEquals(article.getAttribute("myString"), articleLoaded.getAttribute("myString"));
            assertEquals(article.getAttribute("myInt"), articleLoaded.getAttribute("myInt"));
            assertEquals(article.getRelated().size(), articleLoaded.getRelated().size());
            assertEquals(article.getRelated("test").getId(), articleLoaded.getRelated("test").getId());

        } finally {
            db.dropDatabase();
        }
    }

    @Test
    public void testRecursiveReference() throws Exception {
        Mongo mongo = new Mongo();
        DB db = mongo.getDB("morphia_test");
        try {
            DBCollection stuff = db.getCollection("stuff");

            Morphia morphia = new Morphia();
            morphia.map(RecursiveParent.class).map(RecursiveChild.class);

            RecursiveParent parent = new RecursiveParent();
            parent.setCollectionName(stuff.getName());
            BasicDBObject parentDbObj = (BasicDBObject) morphia.toDBObject(parent);
            stuff.save(parentDbObj);

            RecursiveChild child = new RecursiveChild();
            child.setCollectionName(stuff.getName());
            BasicDBObject childDbObj = (BasicDBObject) morphia.toDBObject(child);
            stuff.save(childDbObj);

            RecursiveParent parentLoaded = morphia.fromDBObject(RecursiveParent.class, (BasicDBObject)stuff.findOne(new BasicDBObject(Mapper.ID_KEY, parentDbObj.get(Mapper.ID_KEY))));
            RecursiveChild childLoaded = morphia.fromDBObject(RecursiveChild.class, (BasicDBObject)stuff.findOne(new BasicDBObject(Mapper.ID_KEY, childDbObj.get(Mapper.ID_KEY))));

            parentLoaded.setChild(childLoaded);
            childLoaded.setParent(parentLoaded);

            stuff.save(morphia.toDBObject(parentLoaded));
            stuff.save(morphia.toDBObject(childLoaded));

            RecursiveParent finalParentLoaded = morphia.fromDBObject(RecursiveParent.class, (BasicDBObject)stuff.findOne(new BasicDBObject(Mapper.ID_KEY, parentDbObj.get(Mapper.ID_KEY))));
            RecursiveChild finalChildLoaded = morphia.fromDBObject(RecursiveChild.class, (BasicDBObject)stuff.findOne(new BasicDBObject(Mapper.ID_KEY, childDbObj.get(Mapper.ID_KEY))));

            assertNotNull(finalParentLoaded.getChild());
            assertNotNull(finalChildLoaded.getParent());

        } finally {
            db.dropDatabase();
        }
    }
}
