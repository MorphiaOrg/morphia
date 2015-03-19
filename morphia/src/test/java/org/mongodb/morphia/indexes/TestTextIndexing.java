package org.mongodb.morphia.indexes;

import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Text;
import org.mongodb.morphia.utils.IndexType;

import java.util.List;

public class TestTextIndexing extends TestBase {
    @Entity
    @Indexes(text = @Index(options = @IndexOptions(name = "indexing_test", language = "russian", languageOverride = "nativeTongue")))
    private static class TextIndexAll {
        @Id
        private ObjectId id;
        private String name;
        private String nickName;
    }

    @Entity
    private static class TextIndexing {
        @Id
        private ObjectId id;
        private String name;
        @Text(value = 10, options = @IndexOptions(name = "single_annotation", languageOverride = "nativeTongue"))
        private String nickName;
    }

    @Entity
    @Indexes(text = @Index(fields = {@Field(value = "name", type = IndexType.TEXT),
                                     @Field(value = "nickName", type = IndexType.TEXT, weight = 10),
                                     @Field(value = "age")},
                              options = @IndexOptions(name = "indexing_test", language = "russian", languageOverride = "nativeTongue"))
    )
    private static class SingleTextAnnotation {
        @Id
        private ObjectId id;
        private String name;
        private Integer age;
        private String nickName;
        private String nativeTongue;
    }

    @Test
    public void testIndexAll() {
        getMorphia().map(TextIndexAll.class);
        getDs().ensureIndexes();

        List<DBObject> indexInfo = getDs().getCollection(TextIndexAll.class).getIndexInfo();
        Assert.assertEquals(2, indexInfo.size());
        for (DBObject dbObject : indexInfo) {
            if (!dbObject.get("name").equals("_id_")) {
                Assert.assertEquals(1, ((DBObject) dbObject.get("weights")).get("$**"));
                Assert.assertEquals("russian", dbObject.get("default_language"));
                Assert.assertEquals("nativeTongue", dbObject.get("language_override"));
            }
        }
    }

    @Test
    public void testTextAnnotation() {
        Class<TextIndexing> clazz = TextIndexing.class;
        
        getMorphia().map(clazz);
        getDs().getCollection(clazz).drop();
        getDs().ensureIndexes();

        List<DBObject> indexInfo = getDs().getCollection(clazz).getIndexInfo();
        Assert.assertEquals(2, indexInfo.size());
        boolean found = false;
        for (DBObject dbObject : indexInfo) {
            if (dbObject.get("name").equals("single_annotation")) {
                found = true;
                Assert.assertEquals(dbObject.toString(), "english", dbObject.get("default_language"));
                Assert.assertEquals(dbObject.toString(), "nativeTongue", dbObject.get("language_override"));
                Assert.assertEquals(dbObject.toString(), 10, ((DBObject) dbObject.get("weights")).get("nickName"));
            }
        }
        Assert.assertTrue(found);
        
    }

    private void testIndex(final Class clazz) {
        getMorphia().map(clazz);
        getDs().getCollection(clazz).drop();
        getDs().ensureIndexes();

        List<DBObject> indexInfo = getDs().getCollection(clazz).getIndexInfo();
        Assert.assertEquals(2, indexInfo.size());
        boolean found = false;
        for (DBObject dbObject : indexInfo) {
            if (dbObject.get("name").equals("indexing_test")) {
                found = true;
                Assert.assertEquals(dbObject.toString(), "russian", dbObject.get("default_language"));
                Assert.assertEquals(dbObject.toString(), "nativeTongue", dbObject.get("language_override"));
                Assert.assertEquals(dbObject.toString(), 1, ((DBObject) dbObject.get("weights")).get("name"));
                Assert.assertEquals(dbObject.toString(), 10, ((DBObject) dbObject.get("weights")).get("nickName"));
                Assert.assertEquals(dbObject.toString(), 1, ((DBObject) dbObject.get("key")).get("age"));
            }
        }
        Assert.assertTrue(found);
    }

    @Test
    public void testSingleAnnotation() {
        testIndex(SingleTextAnnotation.class);
    }
}
