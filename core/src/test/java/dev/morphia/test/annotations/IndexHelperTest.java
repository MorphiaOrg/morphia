package dev.morphia.test.annotations;

import com.mongodb.client.MongoCollection;
import dev.morphia.annotations.Collation;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexHelper;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Text;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.ValidationException;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.MappedInterface;
import dev.morphia.test.models.MappedInterfaceImpl;
import dev.morphia.utils.IndexDirection;
import dev.morphia.utils.IndexType;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.CollationAlternate.SHIFTED;
import static com.mongodb.client.model.CollationCaseFirst.UPPER;
import static com.mongodb.client.model.CollationMaxVariable.SPACE;
import static com.mongodb.client.model.CollationStrength.IDENTICAL;
import static dev.morphia.annotations.builders.CollationBuilder.collationBuilder;
import static dev.morphia.annotations.builders.FieldBuilder.fieldBuilder;
import static dev.morphia.annotations.builders.IndexBuilder.indexBuilder;
import static dev.morphia.annotations.builders.IndexOptionsBuilder.indexOptionsBuilder;
import static dev.morphia.annotations.builders.IndexedBuilder.indexedBuilder;
import static dev.morphia.annotations.builders.TextBuilder.textBuilder;
import static java.util.Arrays.asList;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class IndexHelperTest extends TestBase {
    private IndexHelper indexHelper;

    @Test
    public void calculateBadKeys() {
        EntityModel model = getMapper().getEntityModel(IndexedClass.class);
        Index index = indexBuilder()
            .fields(fieldBuilder()
                    .value("texting")
                    .type(IndexType.TEXT)
                    .weight(1)
                    .build(),
                fieldBuilder()
                    .value("nest")
                    .type(IndexType.DESC)
                    .build())
                          .build();
        try {
            getIndexHelper().calculateKeys(model, index);
            fail("Validation should have failed on the bad key");
        } catch (MappingException e) {
            // all good
        }

        index = indexBuilder()
            .fields(fieldBuilder()
                    .value("texting")
                    .type(IndexType.TEXT)
                    .weight(1)
                    .build(),
                fieldBuilder()
                    .value("nest")
                    .type(IndexType.DESC)
                    .build())
                    .options(indexOptionsBuilder().disableValidation(true).build())
                    .build();
        getIndexHelper().calculateKeys(model, index);
    }

    @Test
    public void calculateKeys() {
        EntityModel model = getMapper().getEntityModel(IndexedClass.class);
        Document keys = getIndexHelper().calculateKeys(model, indexBuilder()
            .fields(fieldBuilder()
                    .value("text")
                    .type(IndexType.TEXT)
                    .weight(1)
                    .build(),
                fieldBuilder()
                    .value("nest")
                    .type(IndexType.DESC)
                    .build())
                                                                  .build());
        assertEquals(keys, new Document()
                               .append("text", "text")
                               .append("nest", -1));
    }

    @Test
    public void convertTextIndex() {
        Text text = textBuilder()
                        .value(4)
                        .options(indexOptionsBuilder()
                                     .name("index_name")
                                     .background(true)
                                     .expireAfterSeconds(42)
                                     .sparse(true)
                                     .unique(true)
                                     .build())
                        .build();

        Index index = getIndexHelper().convert(text, "search_field");
        assertEquals(index.options().name(), "index_name");
        assertTrue(index.options().background());
        assertTrue(index.options().sparse());
        assertTrue(index.options().unique());
        assertEquals(fieldBuilder()
                         .value("search_field")
                         .type(IndexType.TEXT)
                         .weight(4)
                         .build(),
            index.fields()[0]);

    }

    @Test
    public void createIndex() {
        String collectionName = getDs().getCollection(IndexedClass.class).getNamespace().getCollectionName();
        MongoCollection<Document> collection = getDatabase().getCollection(collectionName);
        Mapper mapper = getMapper();

        getIndexHelper().createIndex(collection, mapper.getEntityModel(IndexedClass.class));
        List<Document> indexInfo = getIndexInfo(IndexedClass.class);
        List<String> names = new ArrayList<>(asList("latitude_1", "searchme", "indexName_1"));
        for (Document document : indexInfo) {
            String name = document.get("name").toString();
            if (name.equals("latitude_1")) {
                names.remove(name);
                assertEquals(document.get("key"), parse("{ 'latitude' : 1 }"));
            } else if (name.equals("searchme")) {
                names.remove(name);
                assertEquals(document.get("weights"), parse("{ 'text' : 10 }"));
            } else if (name.equals("indexName_1")) {
                names.remove(name);
                assertEquals(document.get("key"), parse("{'indexName': 1 }"));
            } else {
                if (!"_id_".equals(document.get("name"))) {
                    throw new MappingException("Found an index I wasn't expecting:  " + document);
                }
            }
        }
        assertTrue(names.isEmpty(), "Should be empty: " + names);

        collection = getDatabase().getCollection(getDs().getCollection(AbstractParent.class).getNamespace().getCollectionName());
        getIndexHelper().createIndex(collection, mapper.getEntityModel(AbstractParent.class));
        indexInfo = getIndexInfo(AbstractParent.class);
        assertTrue(indexInfo.isEmpty(), "Shouldn't find any indexes: " + indexInfo);

    }

    @Test
    public void findField() {
        getMapper().map(MappedInterface.class, MappedInterfaceImpl.class, AbstractParent.class, IndexedClass.class);
        EntityModel model = getMapper().getEntityModel(IndexedClass.class);

        IndexOptions options = indexOptionsBuilder().build();
        assertEquals(getIndexHelper().findField(model, options, "indexName"), "indexName");
        assertEquals(getIndexHelper().findField(model, options, "nested.name"), "nest.name");
        assertEquals(getIndexHelper().findField(model, options, "nest.name"), "nest.name");

        try {
            assertEquals(getIndexHelper().findField(model, options, "nest.whatsit"), "nest.whatsit");
            fail("Should have failed on the bad index path");
        } catch (ValidationException e) {
            // alles ist gut
        }
        assertEquals(getIndexHelper().findField(model, indexOptionsBuilder().disableValidation(true).build(),
            "nest.whatsit.nested.more.deeply.than.the.object.model"),
            "nest.whatsit.nested.more.deeply.than.the.object.model");
    }

    public IndexHelper getIndexHelper() {
        if (indexHelper == null) {
            indexHelper = new IndexHelper(getMapper());
        }
        return indexHelper;
    }

    @Test
    public void index() {
        MongoCollection<Document> collection = getDatabase().getCollection("indexes");
        EntityModel model = getMapper().getEntityModel(IndexedClass.class);

        IndexOptions options = indexOptionsBuilder()
                                   .name("index_name")
                                   .background(true)
                                   .collation(collation())
                                   .disableValidation(true)
                                   .language("en")
                                   .languageOverride("de")
                                   .sparse(true)
                                   .unique(true)
                                   .build();
        Index index = indexBuilder()
            .fields(fieldBuilder()
                    .value("indexName")
                    .build(),
                fieldBuilder()
                    .value("text")
                    .type(IndexType.DESC)
                    .build())
                          .options(options)
                          .build();
        getIndexHelper().createIndex(collection, model, index);
        List<Document> indexInfo = getIndexInfo(IndexedClass.class);
        for (Document document : indexInfo) {
            if (document.get("name").equals("indexName")) {
                checkIndex(document);

                assertEquals(document.get("default_language"), "en");
                assertEquals(document.get("language_override"), "de");

                assertEquals(document.get("collation"),
                    new Document()
                        .append("locale", "en")
                        .append("caseLevel", true)
                        .append("caseFirst", "upper")
                        .append("strength", 5)
                        .append("numericOrdering", true)
                        .append("alternate", "shifted")
                        .append("maxVariable", "space")
                        .append("backwards", true)
                        .append("normalization", true)
                        .append("version", "57.1"));
            }
        }
    }

    @Test
    public void indexCollationConversion() {
        Collation collation = collation();
        com.mongodb.client.model.Collation driver = getIndexHelper().convert(collation);
        assertEquals(driver.getLocale(), "en");
        assertTrue(driver.getCaseLevel());
        assertEquals(driver.getCaseFirst(), UPPER);
        assertEquals(driver.getStrength(), IDENTICAL);
        assertTrue(driver.getNumericOrdering());
        assertEquals(driver.getAlternate(), SHIFTED);
        assertEquals(driver.getMaxVariable(), SPACE);
        assertTrue(driver.getNormalization());
        assertTrue(driver.getBackwards());
    }

    @Test
    public void indexOptionsConversion() {
        IndexOptions indexOptions = buildOptions(true);
        com.mongodb.client.model.IndexOptions options = getIndexHelper().convert(indexOptions);
        assertEquals(options.getName(), "index_name");
        assertTrue(options.isBackground());
        assertTrue(options.isUnique());
        assertTrue(options.isSparse());
        assertEquals(options.getExpireAfter(TimeUnit.SECONDS), Long.valueOf(42));
        assertEquals(options.getDefaultLanguage(), "en");
        assertEquals(options.getLanguageOverride(), "de");
        assertEquals(getIndexHelper().convert(indexOptions.collation()), options.getCollation());

        assertTrue(getIndexHelper().convert(indexOptions).isBackground());
        assertTrue(getIndexHelper().convert(indexOptions).isBackground());
        assertFalse(getIndexHelper().convert(buildOptions(false)).isBackground());

    }

    @Test
    public void indexPartialFilters() {
        MongoCollection<Document> collection = getDatabase().getCollection("indexes");
        EntityModel model = getMapper().getEntityModel(IndexedClass.class);

        Index index = indexBuilder()
            .fields(fieldBuilder().value("text").build())
                          .options(indexOptionsBuilder()
                                       .partialFilter("{ name : { $gt : 13 } }")
                                       .build())
                          .build();

        getIndexHelper().createIndex(collection, model, index);
        findPartialIndex(Document.parse(index.options().partialFilter()));
    }

    @Test
    public void indexedPartialFilters() {
        MongoCollection<Document> collection = getDatabase().getCollection("indexes");
        EntityModel model = getMapper().getEntityModel(IndexedClass.class);

        Indexed indexed = indexedBuilder()
                              .options(indexOptionsBuilder()
                                           .partialFilter("{ name : { $gt : 13 } }")
                                           .build())
                              .build();

        getIndexHelper().createIndex(collection, model, getIndexHelper().convert(indexed, "text"));
        findPartialIndex(Document.parse(indexed.options().partialFilter()));
    }

    @Test
    public void normalizeIndexed() {
        Indexed indexed = indexedBuilder()
                              .value(IndexDirection.DESC)
                              .options(indexOptionsBuilder().name("index_name")
                                                            .background(true)
                                                            .expireAfterSeconds(42)
                                                            .sparse(true)
                                                            .unique(true)
                                                            .build())
                              .build();

        Index converted = getIndexHelper().convert(indexed, "oldstyle");
        assertEquals(converted.options().name(), "index_name");
        assertTrue(converted.options().background());
        assertTrue(converted.options().sparse());
        assertTrue(converted.options().unique());
        assertEquals(fieldBuilder().value("oldstyle").type(IndexType.DESC).build(), converted.fields()[0]);
    }

    @Test
    public void textPartialFilters() {
        MongoCollection<Document> collection = getDatabase().getCollection("indexes");
        EntityModel model = getMapper().getEntityModel(IndexedClass.class);

        Text text = textBuilder()
                        .value(4)
                        .options(indexOptionsBuilder()
                                     .partialFilter("{ name : { $gt : 13 } }")
                                     .build())
                        .build();

        getIndexHelper().createIndex(collection, model, getIndexHelper().convert(text, "text"));
        findPartialIndex(Document.parse(text.options().partialFilter()));
    }

    @Test(expectedExceptions = MappingException.class)
    public void weightsOnNonTextIndex() {
        MongoCollection<Document> indexes = getDatabase().getCollection("indexes");
        EntityModel model = getMapper().getEntityModel(IndexedClass.class);

        Index index = indexBuilder()
            .fields(fieldBuilder()
                .value("name")
                .weight(10)
                .build())
                          .build();

        getIndexHelper().createIndex(indexes, model, index);
    }

    @Test
    public void wildcardTextIndex() {
        MongoCollection<Document> indexes = getDatabase().getCollection("indexes");
        EntityModel model = getMapper().getEntityModel(IndexedClass.class);

        Index index = indexBuilder()
            .fields(fieldBuilder()
                .value("$**")
                .type(IndexType.TEXT)
                .build())
                          .build();

        getIndexHelper().createIndex(indexes, model, index);

        List<Document> wildcard = getIndexInfo(IndexedClass.class);
        boolean found = false;
        for (Document document : wildcard) {
            found |= document.get("name").equals("$**_text");
        }
        assertTrue(found, "Should have found the wildcard index");
    }

    private IndexOptions buildOptions(boolean background) {
        IndexOptions indexOptions = indexOptionsBuilder()
                                        .name("index_name")
                                        .background(background)
                                        .collation(collation())
                                        .disableValidation(true)
                                        .expireAfterSeconds(42)
                                        .language("en")
                                        .languageOverride("de")
                                        .sparse(true)
                                        .unique(true)
                                        .build();
        return indexOptions;
    }

    private void checkIndex(Document document) {
        assertTrue((Boolean) document.get("background"));
        assertTrue((Boolean) document.get("unique"));
        assertTrue((Boolean) document.get("sparse"));
        assertEquals(document.get("expireAfterSeconds"), 42L);
        assertEquals(document.get("key"), new Document("name", 1).append("text", -1));
    }

    @BeforeMethod
    private void clear() {
        indexHelper = null;
    }

    private Collation collation() {
        return collationBuilder()
                   .alternate(SHIFTED)
                   .backwards(true)
                   .caseFirst(UPPER)
                   .caseLevel(true)
                   .locale("en")
                   .maxVariable(SPACE)
                   .normalization(true)
                   .numericOrdering(true)
                   .strength(IDENTICAL)
                   .build();
    }

    private void findPartialIndex(Document expected) {
        List<Document> indexInfo = getIndexInfo(IndexedClass.class);
        for (Document document : indexInfo) {
            if (!document.get("name").equals("_id_")) {
                assertEquals(document.get("partialFilterExpression"), expected);
            }
        }
    }

    @Entity
    @Indexes(@Index(fields = @Field("indexName")))
    private abstract static class AbstractParent {
        @Id
        private ObjectId id;
        private double indexName;
    }

    @Entity("indexes")
    @Indexes(@Index(fields = @Field("latitude")))
    private static class IndexedClass extends AbstractParent {
        @Text(value = 10, options = @IndexOptions(name = "searchme"))
        private String text;
        private double latitude;
        @Property("nest")
        private MappedInterface nested;
    }

}
