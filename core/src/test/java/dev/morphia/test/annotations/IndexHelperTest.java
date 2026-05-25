package dev.morphia.test.annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.mongodb.client.MongoCollection;

import dev.morphia.annotations.Collation;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Text;
import dev.morphia.annotations.internal.CollationBuilder;
import dev.morphia.annotations.internal.IndexHelper;
import dev.morphia.mapping.IndexDirection;
import dev.morphia.mapping.IndexType;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.ValidationException;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.MappedInterface;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.mongodb.client.model.CollationAlternate.SHIFTED;
import static com.mongodb.client.model.CollationCaseFirst.UPPER;
import static com.mongodb.client.model.CollationMaxVariable.SPACE;
import static com.mongodb.client.model.CollationStrength.IDENTICAL;
import static dev.morphia.annotations.internal.CollationBuilder.collationBuilder;
import static dev.morphia.annotations.internal.FieldBuilder.fieldBuilder;
import static dev.morphia.annotations.internal.IndexBuilder.indexBuilder;
import static dev.morphia.annotations.internal.IndexOptionsBuilder.indexOptionsBuilder;
import static dev.morphia.annotations.internal.IndexedBuilder.indexedBuilder;
import static dev.morphia.annotations.internal.TextBuilder.textBuilder;
import static java.util.Arrays.asList;
import static org.bson.Document.parse;

public class IndexHelperTest extends TestBase {
    private IndexHelper indexHelper;

    @BeforeEach
    public void clean() {
        getDatabase().drop();
    }

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
            Assertions.fail("Validation should have failed on the bad key");
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
        assertDocumentEquals(new Document().append("text", "text").append("nest", -1), keys);
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
        Assertions.assertEquals("index_name", index.options().name());
        Assertions.assertTrue(index.options().background());
        Assertions.assertTrue(index.options().sparse());
        Assertions.assertTrue(index.options().unique());
        Assertions.assertEquals(index.fields()[0], fieldBuilder()
                .value("search_field")
                .type(IndexType.TEXT)
                .weight(4)
                .build());

    }

    @Test
    public void createIndex() {
        List<String> packages = getMapper().getConfig().packages();
        packages.add(IndexedClass.class.getPackageName());
        withConfig(buildConfig().packages(packages), () -> {
            String collectionName = getDs().getCollection(IndexedClass.class).getNamespace().getCollectionName();
            MongoCollection<Document> collection = getDatabase().getCollection(collectionName);
            Mapper mapper = getMapper();
            collection.drop();

            getIndexHelper().createIndex(collection, mapper.getEntityModel(IndexedClass.class));
            List<Document> indexInfo = getIndexInfo(IndexedClass.class);
            List<String> names = new ArrayList<>(asList("latitude_1", "searchme", "indexName_1"));
            for (Document document : indexInfo) {
                String name = document.get("name").toString();
                if (name.equals("latitude_1")) {
                    names.remove(name);
                    Assertions.assertEquals(parse("{ 'latitude' : 1 }"), document.get("key"));
                } else if (name.equals("searchme")) {
                    names.remove(name);
                    Assertions.assertEquals(parse("{ 'text' : 10 }"), document.get("weights"));
                } else if (name.equals("indexName_1")) {
                    names.remove(name);
                    Assertions.assertEquals(parse("{'indexName': 1 }"), document.get("key"));
                } else {
                    if (!"_id_".equals(document.get("name"))) {
                        throw new MappingException("Found an index I wasn't expecting:  " + document);
                    }
                }
            }
            Assertions.assertTrue(names.isEmpty(), "Should be empty: " + names);

            collection = getDatabase().getCollection(getDs().getCollection(AbstractParent.class).getNamespace().getCollectionName());
            getIndexHelper().createIndex(collection, mapper.getEntityModel(AbstractParent.class));
            indexInfo = getIndexInfo(AbstractParent.class);
            Assertions.assertTrue(indexInfo.isEmpty(), "Shouldn't find any indexes: " + indexInfo);

        });
    }

    @Test
    public void findField() {
        EntityModel model = getMapper().getEntityModel(IndexedClass.class);

        IndexOptions options = indexOptionsBuilder().build();
        Assertions.assertEquals("indexName", getIndexHelper().findField(model, options, "indexName"));
        Assertions.assertEquals("nest.name", getIndexHelper().findField(model, options, "nested.name"));
        Assertions.assertEquals("nest.name", getIndexHelper().findField(model, options, "nest.name"));

        try {
            Assertions.assertEquals("nest.whatsit", getIndexHelper().findField(model, options, "nest.whatsit"));
            Assertions.fail("Should have failed on the bad index path");
        } catch (ValidationException e) {
            // alles ist gut
        }
        Assertions.assertEquals("nest.whatsit.nested.more.deeply.than.the.object.model",
                getIndexHelper().findField(model, indexOptionsBuilder().disableValidation(true).build(),
                        "nest.whatsit.nested.more.deeply.than.the.object.model"));
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
                .collation(collation().build())
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

                Assertions.assertEquals("en", document.get("default_language"));
                Assertions.assertEquals("de", document.get("language_override"));

                Assertions.assertEquals(new Document()
                        .append("locale", "en")
                        .append("caseLevel", true)
                        .append("caseFirst", "upper")
                        .append("strength", 5)
                        .append("numericOrdering", true)
                        .append("alternate", "shifted")
                        .append("maxVariable", "space")
                        .append("backwards", true)
                        .append("normalization", true)
                        .append("version", "57.1"), document.get("collation"));
            }
        }
    }

    @Test
    public void indexCollationConversion() {
        Collation collation = collation().build();
        com.mongodb.client.model.Collation driverCollation = getIndexHelper().convert(collation);
        Assertions.assertEquals("en", driverCollation.getLocale());
        Assertions.assertTrue(driverCollation.getCaseLevel());
        Assertions.assertEquals(UPPER, driverCollation.getCaseFirst());
        Assertions.assertEquals(IDENTICAL, driverCollation.getStrength());
        Assertions.assertTrue(driverCollation.getNumericOrdering());
        Assertions.assertEquals(SHIFTED, driverCollation.getAlternate());
        Assertions.assertEquals(SPACE, driverCollation.getMaxVariable());
        Assertions.assertTrue(driverCollation.getNormalization());
        Assertions.assertTrue(driverCollation.getBackwards());

        Assertions.assertNull(getIndexHelper()
                .convert(collation()
                        .locale("")
                        .build()));

        Locale defaultLocale = Locale.getDefault();

        driverCollation = getIndexHelper()
                .convert(collation()
                        .locale(Collation.DEFAULT_LOCALE)
                        .build());
        Assertions.assertEquals(defaultLocale.toString(), driverCollation.getLocale());

        try {
            Locale.setDefault(Locale.CANADA_FRENCH);
            driverCollation = getIndexHelper()
                    .convert(collation()
                            .locale(Collation.DEFAULT_LOCALE)
                            .build());
            Assertions.assertEquals("fr_CA", driverCollation.getLocale());
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void indexOptionsConversion() {
        IndexOptions indexOptions = buildOptions(true);
        com.mongodb.client.model.IndexOptions options = getIndexHelper().convert(indexOptions);
        Assertions.assertEquals("index_name", options.getName());
        Assertions.assertTrue(options.isBackground());
        Assertions.assertTrue(options.isUnique());
        Assertions.assertTrue(options.isSparse());
        Assertions.assertEquals(Long.valueOf(42), options.getExpireAfter(TimeUnit.SECONDS));
        Assertions.assertEquals("en", options.getDefaultLanguage());
        Assertions.assertEquals("de", options.getLanguageOverride());
        Assertions.assertEquals(options.getCollation(), getIndexHelper().convert(indexOptions.collation()));

        Assertions.assertTrue(getIndexHelper().convert(indexOptions).isBackground());
        Assertions.assertTrue(getIndexHelper().convert(indexOptions).isBackground());
        Assertions.assertFalse(getIndexHelper().convert(buildOptions(false)).isBackground());

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
        Assertions.assertEquals("index_name", converted.options().name());
        Assertions.assertTrue(converted.options().background());
        Assertions.assertTrue(converted.options().sparse());
        Assertions.assertTrue(converted.options().unique());
        Assertions.assertEquals(converted.fields()[0], fieldBuilder().value("oldstyle").type(IndexType.DESC).build());
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

    @Test
    public void weightsOnNonTextIndex() {
        Assertions.assertThrows(MappingException.class, () -> {
            MongoCollection<Document> indexes = getDatabase().getCollection("indexes");
            EntityModel model = getMapper().getEntityModel(IndexedClass.class);

            Index index = indexBuilder()
                    .fields(fieldBuilder()
                            .value("name")
                            .weight(10)
                            .build())
                    .build();

            getIndexHelper().createIndex(indexes, model, index);
        });
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
        Assertions.assertTrue(found, "Should have found the wildcard index");
    }

    private IndexOptions buildOptions(boolean background) {
        IndexOptions indexOptions = indexOptionsBuilder()
                .name("index_name")
                .background(background)
                .collation(collation().build())
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
        Assertions.assertTrue((Boolean) document.get("background"));
        Assertions.assertTrue((Boolean) document.get("unique"));
        Assertions.assertTrue((Boolean) document.get("sparse"));
        Assertions.assertEquals(42L, document.get("expireAfterSeconds"));
        Assertions.assertEquals(new Document("name", 1).append("text", -1), document.get("key"));
    }

    @BeforeEach
    private void clear() {
        indexHelper = null;
    }

    private CollationBuilder collation() {
        return collationBuilder()
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

    private void findPartialIndex(Document expected) {
        List<Document> indexInfo = getIndexInfo(IndexedClass.class);
        for (Document document : indexInfo) {
            if (!document.get("name").equals("_id_")) {
                Assertions.assertEquals(expected, document.get("partialFilterExpression"));
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
