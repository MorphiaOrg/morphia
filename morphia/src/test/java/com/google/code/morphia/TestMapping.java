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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import org.junit.Ignore;
import org.junit.Test;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.mapping.MappingException;
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
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

/**
 *
 *
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class TestMapping  extends TestBase {
	
//	@Embedded
//	public static class CustomId {
//		ObjectId id;
//		String type;
//	}
	
	@Entity
	public static class KeyAsId {
		@Id Key<?> id;
		String name = "hello";
		
		protected KeyAsId() {}
		public KeyAsId(Key<?> key) {
			this.id = key;
		}
	}
	
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
	
	@Embedded("no-id")
	public static class RenamedEmbedded {
		String name;
	}
		
	public static class NotEmbeddable {
		String noImNot = "no, I'm not";
	}
	public static class SerializableClass implements Serializable {
		private static final long serialVersionUID = 1L;
		String someString = "hi, from the ether.";
	}

	public static class ContainsRef {
		public @Id String id;
		public DBRef rect;
	}

	public static class HasFinalFieldId{
		public final @Id long id;
		public String name = "some string";
		
		//only called when loaded by the persistence framework.
		protected HasFinalFieldId() {
			id = -1;
		}
		
		public HasFinalFieldId(long id) {
			this.id = id;
		}
	}

	public static class ContainsFinalField{
		public @Id String id;
		public final String name;
		
		protected ContainsFinalField() {
			name = "foo";
		}
		
		public ContainsFinalField(String name) {
			this.name = name;
		}
	}
	
	public static class ContainsbyteArray {
		@Id String id;
		byte[] bytes = "Scott".getBytes();
	}

	public static class ContainsSerializedData{
		@Id String id;
		@Serialized SerializableClass data = new SerializableClass();
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
	
	public static class ContainsPrimitiveMap{
		@Id String id;
		@Embedded public Map<String, Long> embeddedValues = new HashMap();
		public Map<String, Long> values = new HashMap();
	}

	public enum Enum1 { A, B }

	public static class ContainsEnum1KeyMap{
		@Id String id;
		public Map<Enum1, String> values = new HashMap<Enum1,String>();
	}

	public static class ContainsIntKeyMap{
		@Id String id;
		public Map<Integer, String> values = new HashMap<Integer,String>();
	}
	
	public static class ContainsXKeyMap<T>{
		@Id String id;
		public Map<T, String> values = new HashMap<T,String>();
	}
	
	public static abstract class BaseEntity implements Serializable{
		private static final long serialVersionUID = 1L;

		public BaseEntity() {}
	
		@Id String id;
		public String getId() {
			return id;
		}
	
       public void setId(String id) {
    	   this.id = id;
       }
	}
	@Entity
	public static class UsesBaseEntity extends BaseEntity{
		private static final long serialVersionUID = 1L;
		
	}

	public static class MapSubclass extends LinkedHashMap<String, Object> {
		private static final long serialVersionUID = 1L;
		@Id String id;
	}

	@Test
    public void testEnumKeyedMap() throws Exception {
		ContainsEnum1KeyMap map = new ContainsEnum1KeyMap();
		map.values.put(Enum1.A,"I'm a");
		map.values.put(Enum1.B,"I'm b");
		
		Key<?> mapKey = ds.save(map);
		
		ContainsEnum1KeyMap mapLoaded = ds.get(ContainsEnum1KeyMap.class, mapKey.getId());
		
		assertNotNull(mapLoaded);
		assertEquals(2,mapLoaded.values.size());
		assertNotNull(mapLoaded.values.get(Enum1.A));
		assertNotNull(mapLoaded.values.get(Enum1.B));
	}

	@Test
    public void testIntKeyedMap() throws Exception {
		ContainsIntKeyMap map = new ContainsIntKeyMap ();
		map.values.put(1,"I'm 1");
		map.values.put(2,"I'm 2");
		
		Key<?> mapKey = ds.save(map);
		
		ContainsIntKeyMap  mapLoaded = ds.get(ContainsIntKeyMap.class, mapKey.getId());
		
		assertNotNull(mapLoaded);
		assertEquals(2,mapLoaded.values.size());
		assertNotNull(mapLoaded.values.get(1));
		assertNotNull(mapLoaded.values.get(2));
		
		assertNotNull(ds.find(ContainsIntKeyMap.class).field("values.2").exists());
	}

	@Test @Ignore
    public void testGenericKeyedMap() throws Exception {
		ContainsXKeyMap<Integer> map = new ContainsXKeyMap<Integer>();
		map.values.put(1,"I'm 1");
		map.values.put(2,"I'm 2");
		
		Key<?> mapKey = ds.save(map);
		
		ContainsXKeyMap<Integer> mapLoaded = ds.get(ContainsXKeyMap.class, mapKey.getId());
		
		assertNotNull(mapLoaded);
		assertEquals(2,mapLoaded.values.size());
		assertNotNull(mapLoaded.values.get(1));
		assertNotNull(mapLoaded.values.get(2));
	}

	
	@Test
    public void testPrimMap() throws Exception {
		ContainsPrimitiveMap primMap = new ContainsPrimitiveMap();
		primMap.embeddedValues.put("first",1L);
		primMap.embeddedValues.put("second",2L);
		primMap.values.put("first",1L);
		primMap.values.put("second",2L);
		Key<ContainsPrimitiveMap> primMapKey = ds.save(primMap);
		
		ContainsPrimitiveMap primMapLoaded = ds.get(ContainsPrimitiveMap.class, primMapKey.getId());
		
		assertNotNull(primMapLoaded);
		assertEquals(2,primMapLoaded.embeddedValues.size());
		assertEquals(2,primMapLoaded.values.size());
		
		
		
	}

	@Test
    public void testFinalIdField() throws Exception {
		morphia.map(HasFinalFieldId.class);
		Key<HasFinalFieldId> savedKey = ds.save(new HasFinalFieldId(12));
		HasFinalFieldId loaded = ds.get(HasFinalFieldId.class, savedKey.getId());
		assertNotNull(loaded);        
		assertNotNull(loaded.id);        
		assertEquals(loaded.id, 12);
	}

	@Test
    public void testFinalField() throws Exception {
		morphia.map(ContainsFinalField.class);
		Key<ContainsFinalField> savedKey = ds.save(new ContainsFinalField("blah"));
		ContainsFinalField loaded = ds.get(ContainsFinalField.class, savedKey.getId());
		assertNotNull(loaded);        
		assertNotNull(loaded.name);        
		assertEquals("blah",loaded.name);
	}

	@Test
    public void testFinalFieldNotPersisted() throws Exception {
		((DatastoreImpl)ds).getMapper().getOptions().ignoreFinals = true;
		morphia.map(ContainsFinalField.class);
		Key<ContainsFinalField> savedKey = ds.save(new ContainsFinalField("blah"));
		ContainsFinalField loaded = ds.get(ContainsFinalField.class, savedKey.getId());
		assertNotNull(loaded);        
		assertNotNull(loaded.name);        
		assertEquals("foo", loaded.name);
	}

	@Test
    public void testCollectionMapping() throws Exception {
		morphia.map(ContainsCollection.class);
		Key<ContainsCollection> savedKey = ds.save(new ContainsCollection());
		ContainsCollection loaded = ds.get(ContainsCollection.class, savedKey.getId());
		assertEquals(loaded.coll, (new ContainsCollection()).coll);
		assertNotNull(loaded.id);        
	}
	
	@Test
    public void testbyteArrayMapping() throws Exception {
		morphia.map(ContainsbyteArray.class);
		Key<ContainsbyteArray> savedKey = ds.save(new ContainsbyteArray());
		ContainsbyteArray loaded = ds.get(ContainsbyteArray.class, savedKey.getId());
		assertEquals(new String(loaded.bytes), new String((new ContainsbyteArray()).bytes));
		assertNotNull(loaded.id);        
	}
	@Test
    public void testBaseEntityValidity() throws Exception {
		morphia.map(UsesBaseEntity.class);
	}	
	@Test
    public void testSerializedMapping() throws Exception {
		morphia.map(ContainsSerializedData.class);
		Key<ContainsSerializedData> savedKey = ds.save(new ContainsSerializedData());
		ContainsSerializedData loaded = ds.get(ContainsSerializedData.class, savedKey.getId());
		assertNotNull(loaded.data);        		
		assertEquals(loaded.data.someString, (new ContainsSerializedData()).data.someString);
		assertNotNull(loaded.id);        
	}

	@SuppressWarnings("deprecation")
	@Test
    public void testLongArrayMapping() throws Exception {
		morphia.map(ContainsLongAndStringArray.class);
		ds.save(new ContainsLongAndStringArray());
		ContainsLongAndStringArray loaded = ds.<ContainsLongAndStringArray>find(ContainsLongAndStringArray.class).get();
		assertEquals(loaded.longs, (new ContainsLongAndStringArray()).longs);
		assertEquals(loaded.strings, (new ContainsLongAndStringArray()).strings);
		
		ContainsLongAndStringArray clasa = new ContainsLongAndStringArray();
		clasa.strings = new String[] {"a", "B","c"};
		clasa.longs = new Long[] {4L, 5L, 4L};
		Key<ContainsLongAndStringArray> k1 = ds.save(clasa);
		loaded = ds.getByKey(ContainsLongAndStringArray.class, k1);
		assertEquals(loaded.longs, clasa.longs);
		assertEquals(loaded.strings, clasa.strings);
		
		assertNotNull(loaded.id);        
	}
	@Test
    public void testKeyAsId() throws Exception {
        morphia.map(KeyAsId.class);
        
        Rectangle r = new Rectangle(1,1);
//        Rectangle r2 = new Rectangle(11,11);
        
        Key<Rectangle> rKey = ds.save(r);
//        Key<Rectangle> r2Key = ds.save(r2);
        KeyAsId kai = new KeyAsId(rKey);
        Key<KeyAsId> kaiKey = ds.save(kai);
        KeyAsId kaiLoaded = ds.get(KeyAsId.class, rKey);
        assertNotNull(kaiLoaded);
        assertNotNull(kaiKey);
	}
	@Test
    public void testDbRefMapping() throws Exception {
        morphia.map(ContainsRef.class).map(Rectangle.class);
        DBCollection stuff = db.getCollection("stuff");
        DBCollection rectangles = db.getCollection("rectangles");
        
        assertTrue("'ne' field should not be persisted!",
 !morphia.getMappedClasses().get(ContainsRef.class.getName())
				.containsFieldName("ne"));

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
        boolean allGood=false;
        try {
        	morphia.map(MissingId.class);
        } catch (MappingException e) {
        	allGood = true;
        }
        assertTrue("Validation: Missing @Id field not caught", allGood);

        allGood = false;
        try {
        	morphia.map(IdOnEmbedded.class);
        } catch (MappingException e) {
        	allGood = true;
        }
        assertTrue("Validation: @Id field on @Embedded not caught", allGood);

        allGood = false;
        try {
        	morphia.map(RenamedEmbedded.class);
        } catch (MappingException e) {
        	allGood = true;
        }
        assertTrue("Validation: @Embedded(\"name\") not caught on Class", allGood);

        allGood = false;
        try {
        	morphia.map(MissingIdStill.class);
        } catch (MappingException e) {
        	allGood = true;
        }
        assertTrue("Validation: Missing @Id field not not caught", allGood);

        allGood = false;
        try {
        	morphia.map(MissingIdRenamed.class);
        } catch (MappingException e) {
        	allGood = true;
        }
        assertTrue("Validation: Missing @Id field not not caught", allGood);
    }
    
    
    @Test
    public void testBasicMapping() throws Exception {
        try {
            DBCollection hotels = db.getCollection("hotels");
            DBCollection agencies = db.getCollection("agencies");

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
            
            BasicDBObject agencyDbObj = (BasicDBObject) morphia.toDBObject(agency);
            agencies.save(agencyDbObj);

            TravelAgency agencyLoaded = morphia.fromDBObject(TravelAgency.class, (BasicDBObject)agencies.findOne(new BasicDBObject(Mapper.ID_KEY, agencyDbObj.get(Mapper.ID_KEY))));

            assertEquals(agency.getName(), agencyLoaded.getName());
            assertEquals(agency.getHotels().size(), 1);
            assertEquals(agency.getHotels().get(0).getName(), borg.getName());

            // try clearing values
            borgLoaded.setAddress(null);
            borgLoaded.getPhoneNumbers().clear();
            borgLoaded.setName(null);

            hotelDbObj = (BasicDBObject) morphia.toDBObject(borgLoaded);
            hotels.save(hotelDbObj);

            hotelDbObj = (BasicDBObject)hotels.findOne(new BasicDBObject(Mapper.ID_KEY, hotelDbObj.get(Mapper.ID_KEY)));

            borgLoaded = morphia.fromDBObject(Hotel.class, hotelDbObj);
            assertNull(borgLoaded.getAddress());
            assertEquals(0, borgLoaded.getPhoneNumbers().size());
            assertNull(borgLoaded.getName());

        } finally {
            db.dropDatabase();
        }
    }

    @Test
    public void testMaps() throws Exception {
        try {
            DBCollection articles = db.getCollection("articles");
            morphia.map(Article.class).map(Translation.class).map(Circle.class);

            Article related = new Article();
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
        DBCollection stuff = db.getCollection("stuff");

        morphia.map(RecursiveParent.class).map(RecursiveChild.class);

        RecursiveParent parent = new RecursiveParent();
        BasicDBObject parentDbObj = (BasicDBObject) morphia.toDBObject(parent);
        stuff.save(parentDbObj);

        RecursiveChild child = new RecursiveChild();
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
    }
}
