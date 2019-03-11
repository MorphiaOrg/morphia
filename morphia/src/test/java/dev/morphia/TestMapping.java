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
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import org.bson.types.ObjectId;
import org.junit.Ignore;
import org.junit.Test;
import dev.morphia.TestInheritanceMappings.MapLike;
import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Serialized;
import dev.morphia.mapping.DefaultCreator;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.cache.DefaultEntityCache;
import dev.morphia.query.FindOptions;
import dev.morphia.testmodel.Address;
import dev.morphia.testmodel.Article;
import dev.morphia.testmodel.Circle;
import dev.morphia.testmodel.Hotel;
import dev.morphia.testmodel.PhoneNumber;
import dev.morphia.testmodel.Rectangle;
import dev.morphia.testmodel.RecursiveChild;
import dev.morphia.testmodel.RecursiveParent;
import dev.morphia.testmodel.Translation;
import dev.morphia.testmodel.TravelAgency;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 */
public class TestMapping extends TestBase {

    @Test
    public void testAlsoLoad() {
        final ContainsIntegerList cil = new ContainsIntegerList();
        cil.intList.add(1);
        getDs().save(cil);
        final ContainsIntegerList cilLoaded = getDs().get(cil);
        assertNotNull(cilLoaded);
        assertNotNull(cilLoaded.intList);
        assertEquals(cilLoaded.intList.size(), cil.intList.size());
        assertEquals(cilLoaded.intList.get(0), cil.intList.get(0));

        final ContainsIntegerListNew cilNew = getDs().get(ContainsIntegerListNew.class, cil.id);
        assertNotNull(cilNew);
        assertNotNull(cilNew.integers);
        assertEquals(1, cilNew.integers.size());
        assertEquals(1, (int) cil.intList.get(0));
    }

    @Test
    public void testBadMappings() {
        try {
            getMorphia().map(MissingId.class);
            fail("Validation: Missing @Id field not caught");
        } catch (MappingException e) {
            // good
        }

        try {
            getMorphia().map(IdOnEmbedded.class);
            fail("Validation: @Id field on @Embedded not caught");
        } catch (MappingException e) {
            // good
        }

        try {
            getMorphia().map(RenamedEmbedded.class);
            fail("Validation: @Embedded(\"name\") not caught on Class");
        } catch (MappingException e) {
            // good
        }

        try {
            getMorphia().map(MissingIdStill.class);
            fail("Validation: Missing @Id field not not caught");
        } catch (MappingException e) {
            // good
        }

        try {
            getMorphia().map(MissingIdRenamed.class);
            fail("Validation: Missing @Id field not not caught");
        } catch (MappingException e) {
            // good
        }

        try {
            getMorphia().map(NonStaticInnerClass.class);
            fail("Validation: Non-static inner class allowed");
        } catch (MappingException e) {
            // good
        }
    }

    @Test
    public void testBaseEntityValidity() {
        getMorphia().map(UsesBaseEntity.class);
    }

    @Test
    public void testBasicMapping() {
        performBasicMappingTest();
        final DefaultCreator objectFactory = (DefaultCreator) getMorphia().getMapper().getOptions().getObjectFactory();
        assertTrue(objectFactory.getClassNameCache().isEmpty());
    }

    @Test
    public void testBasicMappingWithCachedClasses() {
        getMorphia().getMapper().getOptions().setCacheClassLookups(true);
        try {
            performBasicMappingTest();
            final DefaultCreator objectFactory = (DefaultCreator) getMorphia().getMapper().getOptions().getObjectFactory();
            assertTrue(objectFactory.getClassNameCache().containsKey(Hotel.class.getName()));
            assertTrue(objectFactory.getClassNameCache().containsKey(TravelAgency.class.getName()));
        } finally {
            getMorphia().getMapper().getOptions().setCacheClassLookups(false);
        }
    }

    @Test
    public void testByteArrayMapping() {
        getMorphia().map(ContainsByteArray.class);
        final Key<ContainsByteArray> savedKey = getDs().save(new ContainsByteArray());
        final ContainsByteArray loaded = getDs().get(ContainsByteArray.class, savedKey.getId());
        assertEquals(new String((new ContainsByteArray()).bytes), new String(loaded.bytes));
        assertNotNull(loaded.id);
    }

    @Test
    public void testCollectionMapping() {
        getMorphia().map(ContainsCollection.class);
        final Key<ContainsCollection> savedKey = getDs().save(new ContainsCollection());
        final ContainsCollection loaded = getDs().get(ContainsCollection.class, savedKey.getId());
        assertEquals(loaded.coll, (new ContainsCollection()).coll);
        assertNotNull(loaded.id);
    }

    @Test
    public void testDbRefMapping() {
        getMorphia().map(ContainsRef.class).map(Rectangle.class);
        final DBCollection stuff = getDb().getCollection("stuff");
        final DBCollection rectangles = getDb().getCollection("rectangles");

        assertTrue("'ne' field should not be persisted!", !getMorphia().getMapper().getMCMap().get(ContainsRef.class.getName())
                                                                       .containsJavaFieldName("ne"));

        final Rectangle r = new Rectangle(1, 1);
        final DBObject rDbObject = getMorphia().toDBObject(r);
        rDbObject.put("_ns", rectangles.getName());
        rectangles.save(rDbObject);

        final ContainsRef cRef = new ContainsRef();
        cRef.rect = new DBRef((String) rDbObject.get("_ns"), rDbObject.get("_id"));
        final DBObject cRefDbObject = getMorphia().toDBObject(cRef);
        stuff.save(cRefDbObject);
        final BasicDBObject cRefDbObjectLoaded = (BasicDBObject) stuff.findOne(BasicDBObjectBuilder.start("_id", cRefDbObject.get("_id"))
                                                                                                   .get());
        final ContainsRef cRefLoaded = getMorphia().fromDBObject(getDs(), ContainsRef.class, cRefDbObjectLoaded, new DefaultEntityCache());
        assertNotNull(cRefLoaded);
        assertNotNull(cRefLoaded.rect);
        assertNotNull(cRefLoaded.rect.getId());
        assertNotNull(cRefLoaded.rect.getCollectionName());
        assertEquals(cRefLoaded.rect.getId(), cRef.rect.getId());
        assertEquals(cRefLoaded.rect.getCollectionName(), cRef.rect.getCollectionName());
    }

    @Test
    public void testEmbeddedArrayElementHasNoClassname() {
        getMorphia().map(ContainsEmbeddedArray.class);
        final ContainsEmbeddedArray cea = new ContainsEmbeddedArray();
        cea.res = new RenamedEmbedded[]{new RenamedEmbedded()};

        final DBObject dbObj = getMorphia().toDBObject(cea);
        assertTrue(!((DBObject) ((List) dbObj.get("res")).get(0)).containsField(
            getMorphia().getMapper().getOptions().getDiscriminatorField()));
    }

    @Test
    public void testEmbeddedDBObject() {
        getMorphia().map(ContainsDBObject.class);
        getDs().save(new ContainsDBObject());
        assertNotNull(getDs().find(ContainsDBObject.class)
                             .find(new FindOptions().limit(1))
                             .next());
    }

    @Test
    public void testEmbeddedEntity() {
        getMorphia().map(ContainsEmbeddedEntity.class);
        getDs().save(new ContainsEmbeddedEntity());
        final ContainsEmbeddedEntity ceeLoaded = getDs().find(ContainsEmbeddedEntity.class)
                                                        .find(new FindOptions().limit(1))
                                                        .next();
        assertNotNull(ceeLoaded);
        assertNotNull(ceeLoaded.id);
        assertNotNull(ceeLoaded.cil);
        assertNull(ceeLoaded.cil.id);

    }

    @Test
    public void testEmbeddedEntityDBObjectHasNoClassname() {
        getMorphia().map(ContainsEmbeddedEntity.class);
        final ContainsEmbeddedEntity cee = new ContainsEmbeddedEntity();
        cee.cil = new ContainsIntegerList();
        cee.cil.intList = Collections.singletonList(1);
        final DBObject dbObj = getMorphia().toDBObject(cee);
        assertTrue(!((DBObject) dbObj.get("cil")).containsField(
            getMorphia().getMapper().getOptions().getDiscriminatorField()));
    }

    @Test
    public void testEnumKeyedMap() {
        final ContainsEnum1KeyMap map = new ContainsEnum1KeyMap();
        map.values.put(Enum1.A, "I'm a");
        map.values.put(Enum1.B, "I'm b");
        map.embeddedValues.put(Enum1.A, "I'm a");
        map.embeddedValues.put(Enum1.B, "I'm b");

        final Key<?> mapKey = getDs().save(map);

        final ContainsEnum1KeyMap mapLoaded = getDs().get(ContainsEnum1KeyMap.class, mapKey.getId());

        assertNotNull(mapLoaded);
        assertEquals(2, mapLoaded.values.size());
        assertNotNull(mapLoaded.values.get(Enum1.A));
        assertNotNull(mapLoaded.values.get(Enum1.B));
        assertEquals(2, mapLoaded.embeddedValues.size());
        assertNotNull(mapLoaded.embeddedValues.get(Enum1.A));
        assertNotNull(mapLoaded.embeddedValues.get(Enum1.B));
    }

    @Test
    public void testFinalField() {
        getMorphia().map(ContainsFinalField.class);
        final Key<ContainsFinalField> savedKey = getDs().save(new ContainsFinalField("blah"));
        final ContainsFinalField loaded = getDs().get(ContainsFinalField.class, savedKey.getId());
        assertNotNull(loaded);
        assertNotNull(loaded.name);
        assertEquals("blah", loaded.name);
    }

    @Test
    public void testFinalFieldNotPersisted() {
        getMorphia().getMapper().getOptions().setIgnoreFinals(true);
        getMorphia().map(ContainsFinalField.class);
        final Key<ContainsFinalField> savedKey = getDs().save(new ContainsFinalField("blah"));
        final ContainsFinalField loaded = getDs().get(ContainsFinalField.class, savedKey.getId());
        assertNotNull(loaded);
        assertNotNull(loaded.name);
        assertEquals("foo", loaded.name);
    }

    @Test
    public void testFinalIdField() {
        getMorphia().map(HasFinalFieldId.class);
        final Key<HasFinalFieldId> savedKey = getDs().save(new HasFinalFieldId(12));
        final HasFinalFieldId loaded = getDs().get(HasFinalFieldId.class, savedKey.getId());
        assertNotNull(loaded);
        assertNotNull(loaded.id);
        assertEquals(12, loaded.id);
    }

    @Test
    @Ignore("need to add this feature")
    @SuppressWarnings("unchecked")
    public void testGenericKeyedMap() {
        final ContainsXKeyMap<Integer> map = new ContainsXKeyMap<Integer>();
        map.values.put(1, "I'm 1");
        map.values.put(2, "I'm 2");

        final Key<ContainsXKeyMap<Integer>> mapKey = getDs().save(map);

        final ContainsXKeyMap<Integer> mapLoaded = getDs().get(ContainsXKeyMap.class, mapKey.getId());

        assertNotNull(mapLoaded);
        assertEquals(2, mapLoaded.values.size());
        assertNotNull(mapLoaded.values.get(1));
        assertNotNull(mapLoaded.values.get(2));
    }

    @Test
    public void testIdFieldWithUnderscore() {
        getMorphia().map(StrangelyNamedIdField.class);
    }

    @Test
    public void testIntKeySetStringMap() {
        final ContainsIntKeySetStringMap map = new ContainsIntKeySetStringMap();
        map.values.put(1, Collections.singleton("I'm 1"));
        map.values.put(2, Collections.singleton("I'm 2"));

        final Key<?> mapKey = getDs().save(map);

        final ContainsIntKeySetStringMap mapLoaded = getDs().get(ContainsIntKeySetStringMap.class, mapKey.getId());

        assertNotNull(mapLoaded);
        assertEquals(2, mapLoaded.values.size());
        assertNotNull(mapLoaded.values.get(1));
        assertNotNull(mapLoaded.values.get(2));
        assertEquals(1, mapLoaded.values.get(1).size());

        assertNotNull(getDs().find(ContainsIntKeyMap.class).field("values.2").exists());
        assertEquals(0, getDs().find(ContainsIntKeyMap.class).field("values.2").doesNotExist().count());
        assertNotNull(getDs().find(ContainsIntKeyMap.class).field("values.4").doesNotExist());
        assertEquals(0, getDs().find(ContainsIntKeyMap.class).field("values.4").exists().count());
    }

    @Test
    public void testIntKeyedMap() {
        final ContainsIntKeyMap map = new ContainsIntKeyMap();
        map.values.put(1, "I'm 1");
        map.values.put(2, "I'm 2");

        final Key<?> mapKey = getDs().save(map);

        final ContainsIntKeyMap mapLoaded = getDs().get(ContainsIntKeyMap.class, mapKey.getId());

        assertNotNull(mapLoaded);
        assertEquals(2, mapLoaded.values.size());
        assertNotNull(mapLoaded.values.get(1));
        assertNotNull(mapLoaded.values.get(2));

        assertNotNull(getDs().find(ContainsIntKeyMap.class).field("values.2").exists());
        assertEquals(0, getDs().find(ContainsIntKeyMap.class).field("values.2").doesNotExist().count());
        assertNotNull(getDs().find(ContainsIntKeyMap.class).field("values.4").doesNotExist());
        assertEquals(0, getDs().find(ContainsIntKeyMap.class).field("values.4").exists().count());
    }

    @Test
    public void testIntLists() {
        ContainsIntegerList cil = new ContainsIntegerList();
        getDs().save(cil);
        ContainsIntegerList cilLoaded = getDs().get(cil);
        assertNotNull(cilLoaded);
        assertNotNull(cilLoaded.intList);
        assertEquals(cilLoaded.intList.size(), cil.intList.size());


        cil = new ContainsIntegerList();
        cil.intList = null;
        getDs().save(cil);
        cilLoaded = getDs().get(cil);
        assertNotNull(cilLoaded);
        assertNotNull(cilLoaded.intList);
        assertEquals(0, cilLoaded.intList.size());

        cil = new ContainsIntegerList();
        cil.intList.add(1);
        getDs().save(cil);
        cilLoaded = getDs().get(cil);
        assertNotNull(cilLoaded);
        assertNotNull(cilLoaded.intList);
        assertEquals(1, cilLoaded.intList.size());
        assertEquals(1, (int) cilLoaded.intList.get(0));
    }

    @Test
    public void testLongArrayMapping() {
        getMorphia().map(ContainsLongAndStringArray.class);
        getDs().save(new ContainsLongAndStringArray());
        ContainsLongAndStringArray loaded = getDs().find(ContainsLongAndStringArray.class)
                                                   .find(new FindOptions().limit(1))
                                                   .next();
        assertArrayEquals(loaded.longs, (new ContainsLongAndStringArray()).longs);
        assertArrayEquals(loaded.strings, (new ContainsLongAndStringArray()).strings);

        final ContainsLongAndStringArray array = new ContainsLongAndStringArray();
        array.strings = new String[]{"a", "B", "c"};
        array.longs = new Long[]{4L, 5L, 4L};
        final Key<ContainsLongAndStringArray> k1 = getDs().save(array);
        loaded = getDs().getByKey(ContainsLongAndStringArray.class, k1);
        assertArrayEquals(loaded.longs, array.longs);
        assertArrayEquals(loaded.strings, array.strings);

        assertNotNull(loaded.id);
    }

    @Test
    public void testMapLike() {
        final ContainsMapLike ml = new ContainsMapLike();
        ml.m.put("first", "test");
        getDs().save(ml);
        final ContainsMapLike mlLoaded = getDs().find(ContainsMapLike.class)
                                                .find(new FindOptions().limit(1))
                                                .next();
        assertNotNull(mlLoaded);
        assertNotNull(mlLoaded.m);
        assertNotNull(mlLoaded.m.containsKey("first"));
    }

    @Test
    public void testMapWithEmbeddedInterface() {
        final ContainsMapWithEmbeddedInterface aMap = new ContainsMapWithEmbeddedInterface();
        final Foo f1 = new Foo1();
        final Foo f2 = new Foo2();

        aMap.embeddedValues.put("first", f1);
        aMap.embeddedValues.put("second", f2);
        getDs().save(aMap);

        final ContainsMapWithEmbeddedInterface mapLoaded = getDs().find(ContainsMapWithEmbeddedInterface.class)
                                                                  .find(new FindOptions().limit(1))
                                                                  .next();

        assertNotNull(mapLoaded);
        assertEquals(2, mapLoaded.embeddedValues.size());
        assertTrue(mapLoaded.embeddedValues.get("first") instanceof Foo1);
        assertTrue(mapLoaded.embeddedValues.get("second") instanceof Foo2);

    }

    @Test
    public void testMaps() {
        final DBCollection articles = getDb().getCollection("articles");
        getMorphia().map(Article.class).map(Translation.class).map(Circle.class);

        final Article related = new Article();
        final BasicDBObject relatedDbObj = (BasicDBObject) getMorphia().toDBObject(related);
        articles.save(relatedDbObj);

        final Article relatedLoaded = getMorphia().fromDBObject(getDs(), Article.class,
                                                                articles.findOne(new BasicDBObject("_id",
                                                                                                   relatedDbObj.get("_id"))),
                                                                new DefaultEntityCache());

        final Article article = new Article();
        article.setTranslation("en", new Translation("Hello World", "Just a test"));
        article.setTranslation("is", new Translation("Halló heimur", "Bara að prófa"));

        article.setAttribute("myDate", new Date());
        article.setAttribute("myString", "Test");
        article.setAttribute("myInt", 123);

        article.putRelated("test", relatedLoaded);

        final BasicDBObject articleDbObj = (BasicDBObject) getMorphia().toDBObject(article);
        articles.save(articleDbObj);

        final Article articleLoaded = getMorphia().fromDBObject(getDs(), Article.class,
                                                                articles.findOne(
                                                                                    new BasicDBObject("_id",
                                                                                                      articleDbObj.get("_id"))),
                                                                new DefaultEntityCache());

        assertEquals(article.getTranslations().size(), articleLoaded.getTranslations().size());
        assertEquals(article.getTranslation("en").getTitle(), articleLoaded.getTranslation("en").getTitle());
        assertEquals(article.getTranslation("is").getBody(), articleLoaded.getTranslation("is").getBody());
        assertEquals(article.getAttributes().size(), articleLoaded.getAttributes().size());
        assertEquals(article.getAttribute("myDate"), articleLoaded.getAttribute("myDate"));
        assertEquals(article.getAttribute("myString"), articleLoaded.getAttribute("myString"));
        assertEquals(article.getAttribute("myInt"), articleLoaded.getAttribute("myInt"));
        assertEquals(article.getRelated().size(), articleLoaded.getRelated().size());
        assertEquals(article.getRelated("test").getId(), articleLoaded.getRelated("test").getId());
    }

    @Test
    public void testObjectIdKeyedMap() {
        getMorphia().map(ContainsObjectIdKeyMap.class);
        final ContainsObjectIdKeyMap map = new ContainsObjectIdKeyMap();
        final ObjectId o1 = new ObjectId("111111111111111111111111");
        final ObjectId o2 = new ObjectId("222222222222222222222222");
        map.values.put(o1, "I'm 1s");
        map.values.put(o2, "I'm 2s");

        final Key<?> mapKey = getDs().save(map);

        final ContainsObjectIdKeyMap mapLoaded = getDs().get(ContainsObjectIdKeyMap.class, mapKey.getId());

        assertNotNull(mapLoaded);
        assertEquals(2, mapLoaded.values.size());
        assertNotNull(mapLoaded.values.get(o1));
        assertNotNull(mapLoaded.values.get(o2));

        assertNotNull(getDs().find(ContainsIntKeyMap.class).field("values.111111111111111111111111").exists());
        assertEquals(0, getDs().find(ContainsIntKeyMap.class).field("values.111111111111111111111111").doesNotExist().count());
        assertNotNull(getDs().find(ContainsIntKeyMap.class).field("values.4").doesNotExist());
        assertEquals(0, getDs().find(ContainsIntKeyMap.class).field("values.4").exists().count());
    }

    @Test
    public void testPrimMap() {
        final ContainsPrimitiveMap primMap = new ContainsPrimitiveMap();
        primMap.embeddedValues.put("first", 1L);
        primMap.embeddedValues.put("second", 2L);
        primMap.values.put("first", 1L);
        primMap.values.put("second", 2L);
        final Key<ContainsPrimitiveMap> primMapKey = getDs().save(primMap);

        final ContainsPrimitiveMap primMapLoaded = getDs().get(ContainsPrimitiveMap.class, primMapKey.getId());

        assertNotNull(primMapLoaded);
        assertEquals(2, primMapLoaded.embeddedValues.size());
        assertEquals(2, primMapLoaded.values.size());
    }

    @Test
    public void testPrimMapWithNullValue() {
        final ContainsPrimitiveMap primMap = new ContainsPrimitiveMap();
        primMap.embeddedValues.put("first", null);
        primMap.embeddedValues.put("second", 2L);
        primMap.values.put("first", null);
        primMap.values.put("second", 2L);
        final Key<ContainsPrimitiveMap> primMapKey = getDs().save(primMap);

        final ContainsPrimitiveMap primMapLoaded = getDs().get(ContainsPrimitiveMap.class, primMapKey.getId());

        assertNotNull(primMapLoaded);
        assertEquals(2, primMapLoaded.embeddedValues.size());
        assertEquals(2, primMapLoaded.values.size());
    }

    @Test
    public void testRecursiveReference() {
        final DBCollection stuff = getDb().getCollection("stuff");

        getMorphia().map(RecursiveParent.class).map(RecursiveChild.class);

        final RecursiveParent parent = new RecursiveParent();
        final DBObject parentDbObj = getMorphia().toDBObject(parent);
        stuff.save(parentDbObj);

        final RecursiveChild child = new RecursiveChild();
        final DBObject childDbObj = getMorphia().toDBObject(child);
        stuff.save(childDbObj);

        final RecursiveParent parentLoaded = getMorphia().fromDBObject(getDs(), RecursiveParent.class,
                                                                       stuff.findOne(new BasicDBObject("_id",
                                                                                                       parentDbObj.get("_id"))),
                                                                       new DefaultEntityCache());
        final RecursiveChild childLoaded = getMorphia().fromDBObject(getDs(), RecursiveChild.class,
                                                                     stuff.findOne(new BasicDBObject("_id",
                                                                                                     childDbObj.get("_id"))),
                                                                     new DefaultEntityCache());

        parentLoaded.setChild(childLoaded);
        childLoaded.setParent(parentLoaded);

        stuff.save(getMorphia().toDBObject(parentLoaded));
        stuff.save(getMorphia().toDBObject(childLoaded));

        final RecursiveParent finalParentLoaded = getMorphia().fromDBObject(getDs(), RecursiveParent.class,
                                                                            stuff.findOne(new BasicDBObject("_id",
                                                                                                            parentDbObj.get("_id"))),
                                                                            new DefaultEntityCache());
        final RecursiveChild finalChildLoaded = getMorphia().fromDBObject(getDs(), RecursiveChild.class,
                                                                          stuff.findOne(new BasicDBObject("_id",
                                                                                                          childDbObj.get("_id"))),
                                                                          new DefaultEntityCache());

        assertNotNull(finalParentLoaded.getChild());
        assertNotNull(finalChildLoaded.getParent());
    }

    @Test(expected = MappingException.class)
    public void testReferenceWithoutIdValue() {
        final RecursiveParent parent = new RecursiveParent();
        final RecursiveChild child = new RecursiveChild();
        child.setId(null);
        parent.setChild(child);
        getDs().save(parent);

    }

    @Test
    public void testSerializedMapping() {
        getMorphia().map(ContainsSerializedData.class);
        final Key<ContainsSerializedData> savedKey = getDs().save(new ContainsSerializedData());
        final ContainsSerializedData loaded = getDs().get(ContainsSerializedData.class, savedKey.getId());
        assertNotNull(loaded.data);
        assertEquals(loaded.data.someString, (new ContainsSerializedData()).data.someString);
        assertNotNull(loaded.id);
    }

    @Test
    public void testTimestampMapping() {
        getMorphia().map(ContainsTimestamp.class);
        final ContainsTimestamp cts = new ContainsTimestamp();
        final Key<ContainsTimestamp> savedKey = getDs().save(cts);
        final ContainsTimestamp loaded = getDs().get(ContainsTimestamp.class, savedKey.getId());
        assertNotNull(loaded.ts);
        assertEquals(loaded.ts.getTime(), cts.ts.getTime());

    }

    @Test
    public void testUUID() {
        //       getMorphia().map(ContainsUUID.class);
        final ContainsUUID uuid = new ContainsUUID();
        final UUID before = uuid.uuid;
        getDs().save(uuid);
        final ContainsUUID loaded = getDs().find(ContainsUUID.class)
                                           .find(new FindOptions().limit(1))
                                           .next();
        assertNotNull(loaded);
        assertNotNull(loaded.id);
        assertNotNull(loaded.uuid);
        assertEquals(before, loaded.uuid);
    }

    @Test
    public void testUuidId() {
        getMorphia().map(ContainsUuidId.class);
        final ContainsUuidId uuidId = new ContainsUuidId();
        final UUID before = uuidId.id;
        getDs().save(uuidId);
        final ContainsUuidId loaded = getDs().get(ContainsUuidId.class, before);
        assertNotNull(loaded);
        assertNotNull(loaded.id);
        assertEquals(before, loaded.id);
    }

    private void performBasicMappingTest() {
        final DBCollection hotels = getDb().getCollection("hotels");
        final DBCollection agencies = getDb().getCollection("agencies");

        getMorphia().map(Hotel.class);
        getMorphia().map(TravelAgency.class);

        final Hotel borg = new Hotel();
        borg.setName("Hotel Borg");
        borg.setStars(4);
        borg.setTakesCreditCards(true);
        borg.setStartDate(new Date());
        borg.setType(Hotel.Type.LEISURE);
        borg.getTags().add("Swimming pool");
        borg.getTags().add("Room service");
        borg.setTemp("A temporary transient value");
        borg.getPhoneNumbers().add(new PhoneNumber(354, 5152233, PhoneNumber.Type.PHONE));
        borg.getPhoneNumbers().add(new PhoneNumber(354, 5152244, PhoneNumber.Type.FAX));

        final Address address = new Address();
        address.setStreet("Posthusstraeti 11");
        address.setPostCode("101");
        borg.setAddress(address);

        BasicDBObject hotelDbObj = (BasicDBObject) getMorphia().toDBObject(borg);
        assertTrue(!(((DBObject) ((List) hotelDbObj.get("phoneNumbers")).get(0)).containsField(
            getMorphia().getMapper().getOptions().getDiscriminatorField())));


        hotels.save(hotelDbObj);

        Hotel borgLoaded = getMorphia().fromDBObject(getDs(), Hotel.class, hotelDbObj, new DefaultEntityCache());

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

        final TravelAgency agency = new TravelAgency();
        agency.setName("Lastminute.com");
        agency.getHotels().add(borgLoaded);

        final BasicDBObject agencyDbObj = (BasicDBObject) getMorphia().toDBObject(agency);
        agencies.save(agencyDbObj);

        final TravelAgency agencyLoaded = getMorphia().fromDBObject(getDs(), TravelAgency.class,
                                                                    agencies.findOne(new BasicDBObject("_id",
                                                                                                       agencyDbObj.get("_id"))),
                                                                    new DefaultEntityCache());

        assertEquals(agency.getName(), agencyLoaded.getName());
        assertEquals(1, agency.getHotels().size());
        assertEquals(agency.getHotels().get(0).getName(), borg.getName());

        // try clearing values
        borgLoaded.setAddress(null);
        borgLoaded.getPhoneNumbers().clear();
        borgLoaded.setName(null);

        hotelDbObj = (BasicDBObject) getMorphia().toDBObject(borgLoaded);
        hotels.save(hotelDbObj);

        hotelDbObj = (BasicDBObject) hotels.findOne(new BasicDBObject("_id", hotelDbObj.get("_id")));

        borgLoaded = getMorphia().fromDBObject(getDs(), Hotel.class, hotelDbObj, new DefaultEntityCache());
        assertNull(borgLoaded.getAddress());
        assertEquals(0, borgLoaded.getPhoneNumbers().size());
        assertNull(borgLoaded.getName());
    }

    public enum Enum1 {
        A,
        B
    }

    private interface Foo {
    }

    public abstract static class BaseEntity implements Serializable {
        @Id
        private ObjectId id;

        public String getId() {
            return id.toString();
        }

        public void setId(final String id) {
            this.id = new ObjectId(id);
        }
    }

    @Entity
    public static class MissingId {
        private String id;
    }

    private static class MissingIdStill {
        private String id;
    }

    @Entity("no-id")
    private static class MissingIdRenamed {
        private String id;
    }

    @Embedded
    private static class IdOnEmbedded {
        @Id
        private ObjectId id;
    }

    @Embedded("no-id")
    private static class RenamedEmbedded {
        private String name;
    }

    private static class StrangelyNamedIdField {
        //CHECKSTYLE:OFF
        @Id
        private ObjectId id_ = new ObjectId();
        //CHECKSTYLE:ON
    }

    private static class ContainsEmbeddedArray {
        @Id
        private ObjectId id = new ObjectId();
        private RenamedEmbedded[] res;
    }

    private static class NotEmbeddable {
        private String noImNot = "no, I'm not";
    }

    private static class SerializableClass implements Serializable {
        private final String someString = "hi, from the ether.";
    }

    private static class ContainsRef {
        @Id
        private ObjectId id;
        private DBRef rect;
    }

    private static class HasFinalFieldId {
        @Id
        private final long id;
        private String name = "some string";

        //only called when loaded by the persistence framework.
        protected HasFinalFieldId() {
            id = -1;
        }

        HasFinalFieldId(final long id) {
            this.id = id;
        }
    }

    private static class ContainsFinalField {
        private final String name;
        @Id
        private ObjectId id;

        protected ContainsFinalField() {
            name = "foo";
        }

        ContainsFinalField(final String name) {
            this.name = name;
        }
    }

    private static class ContainsTimestamp {
        private final Timestamp ts = new Timestamp(System.currentTimeMillis());
        @Id
        private ObjectId id;
    }

    private static class ContainsDBObject {
        @Id
        private ObjectId id;
        private DBObject dbObj = BasicDBObjectBuilder.start("field", "val").get();
    }

    private static class ContainsByteArray {
        private final byte[] bytes = "Scott".getBytes();
        @Id
        private ObjectId id;
    }

    private static class ContainsSerializedData {
        @Serialized
        private final SerializableClass data = new SerializableClass();
        @Id
        private ObjectId id;
    }

    private static class ContainsLongAndStringArray {
        @Id
        private ObjectId id;
        private Long[] longs = {0L, 1L, 2L};
        private String[] strings = {"Scott", "Rocks"};
    }

    private static final class ContainsCollection {
        private final Collection<String> coll = new ArrayList<String>();
        @Id
        private ObjectId id;

        private ContainsCollection() {
            coll.add("hi");
            coll.add("Scott");
        }
    }

    private static class ContainsPrimitiveMap {
        @Embedded
        private final Map<String, Long> embeddedValues = new HashMap<String, Long>();
        private final Map<String, Long> values = new HashMap<String, Long>();
        @Id
        private ObjectId id;
    }

    private static class Foo1 implements Foo {
        private String s;
    }

    private static class Foo2 implements Foo {
        private int i;
    }

    private static class ContainsMapWithEmbeddedInterface {
        @Embedded
        private final Map<String, Foo> embeddedValues = new HashMap<String, Foo>();
        @Id
        private ObjectId id;
    }

    private static class ContainsEmbeddedEntity {
        @Id
        private final ObjectId id = new ObjectId();
        @Embedded
        private ContainsIntegerList cil = new ContainsIntegerList();
    }

    @Entity(value = "cil", noClassnameStored = true)
    private static class ContainsIntegerList {
        @Id
        private ObjectId id;
        private List<Integer> intList = new ArrayList<Integer>();
    }

    private static class ContainsIntegerListNewAndOld {
        @Id
        private ObjectId id;
        private List<Integer> intList = new ArrayList<Integer>();
        private List<Integer> integers = new ArrayList<Integer>();
    }

    @Entity(value = "cil", noClassnameStored = true)
    private static class ContainsIntegerListNew {
        @AlsoLoad("intList")
        private final List<Integer> integers = new ArrayList<Integer>();
        @Id
        private ObjectId id;
    }

    @Entity(noClassnameStored = true)
    private static class ContainsUUID {
        private final UUID uuid = UUID.randomUUID();
        @Id
        private ObjectId id;
    }

    @Entity(noClassnameStored = true)
    private static class ContainsUuidId {
        @Id
        private final UUID id = UUID.randomUUID();
    }

    private static class ContainsEnum1KeyMap {
        private final Map<Enum1, String> values = new HashMap<Enum1, String>();
        @Embedded
        private final Map<Enum1, String> embeddedValues = new HashMap<Enum1, String>();
        @Id
        private ObjectId id;
    }

    private static class ContainsIntKeyMap {
        private final Map<Integer, String> values = new HashMap<Integer, String>();
        @Id
        private ObjectId id;
    }

    private static class ContainsIntKeySetStringMap {
        @Embedded
        private final Map<Integer, Set<String>> values = new HashMap<Integer, Set<String>>();
        @Id
        private ObjectId id;
    }

    private static class ContainsObjectIdKeyMap {
        private final Map<ObjectId, String> values = new HashMap<ObjectId, String>();
        @Id
        private ObjectId id;
    }

    private static class ContainsXKeyMap<T> {
        private final Map<T, String> values = new HashMap<T, String>();
        @Id
        private ObjectId id;
    }

    private static class ContainsMapLike {
        private final MapLike m = new MapLike();
        @Id
        private ObjectId id;
    }

    @Entity
    private static class UsesBaseEntity extends BaseEntity {

    }

    private static class MapSubclass extends LinkedHashMap<String, Object> {
        @Id
        private ObjectId id;
    }

    private class NonStaticInnerClass {
        @Id
        private long id = 1;
    }
}
