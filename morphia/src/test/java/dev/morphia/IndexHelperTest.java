/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.morphia;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import dev.morphia.annotations.Collation;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Text;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.utils.IndexDirection;
import dev.morphia.utils.IndexType;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mongodb.BasicDBObject.parse;
import static com.mongodb.client.model.CollationAlternate.SHIFTED;
import static com.mongodb.client.model.CollationCaseFirst.UPPER;
import static com.mongodb.client.model.CollationMaxVariable.SPACE;
import static com.mongodb.client.model.CollationStrength.IDENTICAL;
import static com.mongodb.client.model.CollationStrength.SECONDARY;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IndexHelperTest extends TestBase {
    private final IndexHelper indexHelper = new IndexHelper(getMorphia().getMapper(), getDatabase());

    @Before
    public void before() {
        getMorphia().map(AbstractParent.class, IndexedClass.class, NestedClass.class, NestedClassImpl.class);
    }

    @Test
    public void calculateBadKeys() {
        MappedClass mappedClass = getMorphia().getMapper().getMappedClass(IndexedClass.class);
        IndexBuilder index = new IndexBuilder()
            .fields(new FieldBuilder()
                        .value("texting")
                        .type(IndexType.TEXT)
                        .weight(1),
                    new FieldBuilder()
                        .value("nest")
                        .type(IndexType.DESC));
        try {
            indexHelper.calculateKeys(mappedClass, index);
            fail("Validation should have failed on the bad key");
        } catch (MappingException e) {
            // all good
        }

        index.options(new IndexOptionsBuilder().disableValidation(true));
        indexHelper.calculateKeys(mappedClass, index);
    }

    @Test
    public void calculateKeys() {
        MappedClass mappedClass = getMorphia().getMapper().getMappedClass(IndexedClass.class);
        BsonDocument keys = indexHelper.calculateKeys(mappedClass, new IndexBuilder()
            .fields(new FieldBuilder()
                        .value("text")
                        .type(IndexType.TEXT)
                        .weight(1),
                    new FieldBuilder()
                        .value("nest")
                        .type(IndexType.DESC)));
        assertEquals(new BsonDocument()
                         .append("text", new BsonString("text"))
                         .append("nest", new BsonInt32(-1)),
                     keys);
    }

    @Test
    public void createIndex() {
        assumeMinServerVersion(3.4);
        String collectionName = getDs().getCollection(IndexedClass.class).getName();
        MongoCollection<Document> collection = getDatabase().getCollection(collectionName);
        Mapper mapper = getMorphia().getMapper();

        indexHelper.createIndex(collection, mapper.getMappedClass(IndexedClass.class), false);
        List<DBObject> indexInfo = getDs().getCollection(IndexedClass.class)
                                          .getIndexInfo();
        assertEquals("Should have 6 indexes", 6, indexInfo.size());
        for (DBObject dbObject : indexInfo) {
            String name = dbObject.get("name").toString();
            if (name.equals("latitude_1")) {
                assertEquals(parse("{ 'latitude' : 1 }"), dbObject.get("key"));
            } else if (name.equals("behind_interface")) {
                assertEquals(parse("{ 'nest.name' : -1} "), dbObject.get("key"));
                assertEquals(parse("{ 'locale' : 'en' , 'caseLevel' : false , 'caseFirst' : 'off' , 'strength' : 2 , 'numericOrdering' :"
                                       + " false , 'alternate' : 'non-ignorable' , 'maxVariable' : 'punct' , 'normalization' : false , "
                                       + "'backwards' : false , 'version' : '57.1'}"), dbObject.get("collation"));
            } else if (name.equals("nest.name_1")) {
                assertEquals(parse("{ 'nest.name' : 1} "), dbObject.get("key"));
            } else if (name.equals("searchme")) {
                assertEquals(parse("{ 'text' : 10 }"), dbObject.get("weights"));
            } else if (name.equals("indexName_1")) {
                assertEquals(parse("{'indexName': 1 }"), dbObject.get("key"));
            } else {
                if (!"_id_".equals(dbObject.get("name"))) {
                    throw new MappingException("Found an index I wasn't expecting:  " + dbObject);
                }
            }
        }

        collection = getDatabase().getCollection(getDs().getCollection(AbstractParent.class).getName());
        indexHelper.createIndex(collection, mapper.getMappedClass(AbstractParent.class), false);
        indexInfo = getDs().getCollection(AbstractParent.class).getIndexInfo();
        assertTrue("Shouldn't find any indexes: " + indexInfo, indexInfo.isEmpty());

    }

    @Test
    public void findField() {
        MappedClass mappedClass = getMorphia().getMapper().getMappedClass(IndexedClass.class);

        assertEquals("indexName", indexHelper.findField(mappedClass, new IndexOptionsBuilder(), singletonList("indexName")));
        assertEquals("nest.name", indexHelper.findField(mappedClass, new IndexOptionsBuilder(), asList("nested", "name")));
        assertEquals("nest.name", indexHelper.findField(mappedClass, new IndexOptionsBuilder(), asList("nest", "name")));

        try {
            assertEquals("nest.whatsit", indexHelper.findField(mappedClass, new IndexOptionsBuilder(), asList("nest", "whatsit")));
            fail("Should have failed on the bad index path");
        } catch (MappingException e) {
            // alles ist gut
        }
        assertEquals("nest.whatsit.nested.more.deeply.than.the.object.model",
                     indexHelper.findField(mappedClass, new IndexOptionsBuilder().disableValidation(true),
                                           asList("nest", "whatsit", "nested", "more", "deeply", "than", "the", "object", "model")));
    }

    @Test
    public void index() {
        assumeMinServerVersion(3.4);
        assumeServerIsAtMostVersion(4.0);
        MongoCollection<Document> indexes = getDatabase().getCollection("indexes");
        MappedClass mappedClass = getMorphia().getMapper().getMappedClass(IndexedClass.class);

        indexes.drop();
        Index index = new IndexBuilder()
            .fields(new FieldBuilder()
                        .value("indexName"),
                    new FieldBuilder()
                        .value("text")
                        .type(IndexType.DESC))
            .options(indexOptions());
        indexHelper.createIndex(indexes, mappedClass, index, false);
        List<DBObject> indexInfo = getDs().getCollection(IndexedClass.class)
                                          .getIndexInfo();
        for (DBObject dbObject : indexInfo) {
            if (dbObject.get("name").equals("indexName")) {
                checkIndex(dbObject);

                assertEquals("en", dbObject.get("default_language"));
                assertEquals("de", dbObject.get("language_override"));

                assertEquals(new BasicDBObject()
                                 .append("locale", "en")
                                 .append("caseLevel", true)
                                 .append("caseFirst", "upper")
                                 .append("strength", 5)
                                 .append("numericOrdering", true)
                                 .append("alternate", "shifted")
                                 .append("maxVariable", "space")
                                 .append("backwards", true)
                                 .append("normalization", true)
                                 .append("version", "57.1"),
                             dbObject.get("collation"));
            }
        }
    }

    @Test
    public void indexCollationConversion() {
        Collation collation = collation();
        com.mongodb.client.model.Collation driver = indexHelper.convert(collation);
        assertEquals("en", driver.getLocale());
        assertTrue(driver.getCaseLevel());
        assertEquals(UPPER, driver.getCaseFirst());
        assertEquals(IDENTICAL, driver.getStrength());
        assertTrue(driver.getNumericOrdering());
        assertEquals(SHIFTED, driver.getAlternate());
        assertEquals(SPACE, driver.getMaxVariable());
        assertTrue(driver.getNormalization());
        assertTrue(driver.getBackwards());
    }

    @Test
    public void indexOptionsConversion() {
        IndexOptionsBuilder indexOptions = indexOptions();
        com.mongodb.client.model.IndexOptions options = indexHelper.convert(indexOptions, false);
        assertEquals("index_name", options.getName());
        assertTrue(options.isBackground());
        assertTrue(options.isUnique());
        assertTrue(options.isSparse());
        assertEquals(Long.valueOf(42), options.getExpireAfter(TimeUnit.SECONDS));
        assertEquals("en", options.getDefaultLanguage());
        assertEquals("de", options.getLanguageOverride());
        assertEquals(indexHelper.convert(indexOptions.collation()), options.getCollation());

        assertTrue(indexHelper.convert(indexOptions, true).isBackground());
        assertTrue(indexHelper.convert(indexOptions.background(false), true).isBackground());
        assertTrue(indexHelper.convert(indexOptions.background(true), true).isBackground());
        assertTrue(indexHelper.convert(indexOptions.background(true), false).isBackground());
        assertFalse(indexHelper.convert(indexOptions.background(false), false).isBackground());

    }

    @Test
    public void oldIndexForm() {
        assumeServerIsAtMostVersion(4.0);
        MongoCollection<Document> indexes = getDatabase().getCollection("indexes");
        MappedClass mappedClass = getMorphia().getMapper().getMappedClass(IndexedClass.class);

        indexes.drop();
        Index index = new IndexBuilder()
            .name("index_name")
            .background(true)
            .disableValidation(true)
            .expireAfterSeconds(42)
            .sparse(true)
            .unique(true)
            .value("indexName, -text");
        indexHelper.createIndex(indexes, mappedClass, index, false);
        List<DBObject> indexInfo = getDs().getCollection(IndexedClass.class)
                                          .getIndexInfo();
        for (DBObject dbObject : indexInfo) {
            if (dbObject.get("name").equals("index_indexName")) {
                checkIndex(dbObject);
            }
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void oldIndexedForm() {
        Indexed indexed = new IndexedBuilder()
            .name("index_name")
            .background(true)
            .dropDups(true)
            .expireAfterSeconds(42)
            .sparse(true)
            .unique(true)
            .value(IndexDirection.DESC);
        assertEquals(indexed.options().name(), "");

        Index converted = indexHelper.convert(indexed, "oldstyle");
        assertEquals(converted.options().name(), "index_name");
        assertTrue(converted.options().background());
        assertTrue(converted.options().dropDups());
        assertTrue(converted.options().sparse());
        assertTrue(converted.options().unique());
        assertEquals(new FieldBuilder().value("oldstyle").type(IndexType.DESC), converted.fields()[0]);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void convertTextIndex() {
        TextBuilder text = new TextBuilder()
            .value(4)
            .options(new IndexOptionsBuilder()
                         .name("index_name")
                         .background(true)
                         .dropDups(true)
                         .expireAfterSeconds(42)
                         .sparse(true)
                         .unique(true));

        Index index = indexHelper.convert(text, "search_field");
        assertEquals(index.options().name(), "index_name");
        assertTrue(index.options().background());
        assertTrue(index.options().dropDups());
        assertTrue(index.options().sparse());
        assertTrue(index.options().unique());
        assertEquals(new FieldBuilder()
                         .value("search_field")
                         .type(IndexType.TEXT)
                         .weight(4),
                     index.fields()[0]);

    }

    @Test
    @SuppressWarnings("deprecation")
    public void normalizeIndexed() {
        Indexed indexed = new IndexedBuilder()
            .value(IndexDirection.DESC)
            .options(new IndexOptionsBuilder().name("index_name")
                                              .background(true)
                                              .dropDups(true)
                                              .expireAfterSeconds(42)
                                              .sparse(true)
                                              .unique(true));

        Index converted = indexHelper.convert(indexed, "oldstyle");
        assertEquals(converted.options().name(), "index_name");
        assertTrue(converted.options().background());
        assertTrue(converted.options().dropDups());
        assertTrue(converted.options().sparse());
        assertTrue(converted.options().unique());
        assertEquals(new FieldBuilder().value("oldstyle").type(IndexType.DESC), converted.fields()[0]);
    }

    @Test
    public void wildcardTextIndex() {
        MongoCollection<Document> indexes = getDatabase().getCollection("indexes");
        MappedClass mappedClass = getMorphia().getMapper().getMappedClass(IndexedClass.class);

        IndexBuilder index = new IndexBuilder()
            .fields(new FieldBuilder()
                        .value("$**")
                        .type(IndexType.TEXT));

        indexHelper.createIndex(indexes, mappedClass, index, false);

        List<DBObject> wildcard = getDb().getCollection("indexes").getIndexInfo();
        boolean found = false;
        for (DBObject dbObject : wildcard) {
            found |= dbObject.get("name").equals("$**_text");
        }
        assertTrue("Should have found the wildcard index", found);
    }

    @Test(expected = MappingException.class)
    public void weightsOnNonTextIndex() {
        MongoCollection<Document> indexes = getDatabase().getCollection("indexes");
        MappedClass mappedClass = getMorphia().getMapper().getMappedClass(IndexedClass.class);

        IndexBuilder index = new IndexBuilder()
            .fields(new FieldBuilder()
                        .value("name")
                        .weight(10));

        indexHelper.createIndex(indexes, mappedClass, index, false);
    }

    @Test
    public void indexPartialFilters() {
        MongoCollection<Document> collection = getDatabase().getCollection("indexes");
        MappedClass mappedClass = getMorphia().getMapper().getMappedClass(IndexedClass.class);

        Index index = new IndexBuilder()
            .fields(new FieldBuilder().value("text"))
            .options(new IndexOptionsBuilder()
                         .partialFilter("{ name : { $gt : 13 } }"));

        indexHelper.createIndex(collection, mappedClass, index, false);
        findPartialIndex(BasicDBObject.parse(index.options().partialFilter()));
    }
    @Test
    public void indexedPartialFilters() {
        MongoCollection<Document> collection = getDatabase().getCollection("indexes");
        MappedClass mappedClass = getMorphia().getMapper().getMappedClass(IndexedClass.class);

        Indexed indexed = new IndexedBuilder()
            .options(new IndexOptionsBuilder()
                         .partialFilter("{ name : { $gt : 13 } }"));

        indexHelper.createIndex(collection, mappedClass, indexHelper.convert(indexed, "text"), false);
        findPartialIndex(BasicDBObject.parse(indexed.options().partialFilter()));
    }

    @Test
    public void textPartialFilters() {
        MongoCollection<Document> collection = getDatabase().getCollection("indexes");
        MappedClass mappedClass = getMorphia().getMapper().getMappedClass(IndexedClass.class);

        Text text = new TextBuilder()
            .value(4)
            .options(new IndexOptionsBuilder()
                         .partialFilter("{ name : { $gt : 13 } }"));

        indexHelper.createIndex(collection, mappedClass, indexHelper.convert(text, "text"), false);
        findPartialIndex(BasicDBObject.parse(text.options().partialFilter()));
    }

    private void checkIndex(final DBObject dbObject) {
        assertTrue((Boolean) dbObject.get("background"));
        assertTrue((Boolean) dbObject.get("unique"));
        assertTrue((Boolean) dbObject.get("sparse"));
        assertEquals(42L, dbObject.get("expireAfterSeconds"));
        assertEquals(new BasicDBObject("name", 1).append("text", -1), dbObject.get("key"));
    }

    private void findPartialIndex(final BasicDBObject expected) {
        List<DBObject> indexInfo = getDs().getCollection(IndexedClass.class)
                                          .getIndexInfo();
        for (DBObject dbObject : indexInfo) {
            if (!dbObject.get("name").equals("_id_")) {
                Assert.assertEquals(expected, dbObject.get("partialFilterExpression"));
            }
        }
    }

    private Collation collation() {
        return new CollationBuilder()
            .alternate(SHIFTED)
            .backwards(true)
            .caseFirst(UPPER)
            .caseLevel(true)
            .locale("en")
            .maxVariable(SPACE)
            .normalization(true)
            .numericOrdering(true)
            .strength(IDENTICAL);
    }

    private IndexOptionsBuilder indexOptions() {
        return new IndexOptionsBuilder()
            .name("index_name")
            .background(true)
            .collation(collation())
            .disableValidation(true)
            .dropDups(true)
            .expireAfterSeconds(42)
            .language("en")
            .languageOverride("de")
            .sparse(true)
            .unique(true);
    }

    @Embedded
    private interface NestedClass {
    }

    @Entity("indexes")
    @Indexes(@Index(fields = @Field("latitude")))
    private static class IndexedClass extends AbstractParent {
        @Text(value = 10, options = @IndexOptions(name = "searchme"))
        private String text;
        private double latitude;
        @Embedded("nest")
        private NestedClass nested;
    }

    @Indexes(
        @Index(fields = @Field(value = "name", type = IndexType.DESC),
            options = @IndexOptions(name = "behind_interface",
                collation = @Collation(locale = "en", strength = SECONDARY))))
    private static class NestedClassImpl implements NestedClass {
        @Indexed
        private String name;
    }

    @Indexes(@Index(fields = @Field("indexName")))
    private abstract static class AbstractParent {
        @Id
        private ObjectId id;
        private double indexName;
    }
}
