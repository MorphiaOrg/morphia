package dev.morphia.indexes;

import com.mongodb.MongoCommandException;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Text;
import dev.morphia.mapping.Mapper;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static dev.morphia.utils.IndexType.TEXT;

public class TestTextIndexing extends TestBase {
    @Test(expected = MongoCommandException.class)
    public void shouldNotAllowMultipleTextIndexes() {
        Class<MultipleTextIndexes> clazz = MultipleTextIndexes.class;
        getMapper().map(clazz);
        getMapper().getCollection(clazz).drop();
        getDs().ensureIndexes();
    }

    @Test
    public void testIndexAll() {
        getMapper().map(TextIndexAll.class);
        getDs().ensureIndexes();

        List<Document> indexInfo = getIndexInfo(TextIndexAll.class);
        Assert.assertEquals(2, indexInfo.size());
        for (Document document : indexInfo) {
            if (!document.get("name").equals("_id_")) {
                Assert.assertEquals(1, ((Document) document.get("weights")).get("$**"));
                Assert.assertEquals("english", document.get("default_language"));
                Assert.assertEquals("language", document.get("language_override"));
            }
        }
    }

    @Test
    public void testSingleAnnotation() {
        getMapper().map(CompoundTextIndex.class);
        getMapper().getCollection(CompoundTextIndex.class).drop();
        getDs().ensureIndexes();

        List<Document> indexInfo = getIndexInfo(CompoundTextIndex.class);
        Assert.assertEquals(2, indexInfo.size());
        boolean found = false;
        for (Document document : indexInfo) {
            if (document.get("name").equals("indexing_test")) {
                found = true;
                Assert.assertEquals(document.toString(), "russian", document.get("default_language"));
                Assert.assertEquals(document.toString(), "nativeTongue", document.get("language_override"));
                Assert.assertEquals(document.toString(), 1, ((Document) document.get("weights")).get("name"));
                Assert.assertEquals(document.toString(), 10, ((Document) document.get("weights")).get("nick"));
                Assert.assertEquals(document.toString(), 1, ((Document) document.get("key")).get("age"));
            }
        }
        Assert.assertTrue(found);
    }

    @Test
    public void testTextAnnotation() {
        Class<SingleFieldTextIndex> clazz = SingleFieldTextIndex.class;

        getMapper().map(clazz);
        getMapper().getCollection(clazz).drop();
        getDs().ensureIndexes();

        List<Document> indexInfo = getIndexInfo(clazz);
        Assert.assertEquals(indexInfo.toString(), 2, indexInfo.size());
        boolean found = false;
        for (Document document : indexInfo) {
            if (document.get("name").equals("single_annotation")) {
                found = true;
                Assert.assertEquals(document.toString(), "english", document.get("default_language"));
                Assert.assertEquals(document.toString(), "nativeTongue", document.get("language_override"));
                Assert.assertEquals(document.toString(), 10, ((Document) document.get("weights")).get("nickName"));
            }
        }
        Assert.assertTrue(indexInfo.toString(), found);

    }

    @Entity
    @Indexes(@Index(fields = @Field(value = "$**", type = TEXT)))
    private static class TextIndexAll {
        @Id
        private ObjectId id;
        private String name;
        private String nickName;
    }

    @Entity
    @Indexes(@Index(fields = {@Field(value = "name", type = TEXT),
                              @Field(value = "nick", type = TEXT, weight = 10),
                              @Field(value = "age")}, options = @IndexOptions(name = "indexing_test", language = "russian",
                                                                                                     languageOverride = "nativeTongue")))
    private static class CompoundTextIndex {
        @Id
        private ObjectId id;
        private String name;
        private Integer age;
        @Property("nick")
        private String nickName;
        private String nativeTongue;
    }

    @Entity
    private static class SingleFieldTextIndex {
        @Id
        private ObjectId id;
        private String name;
        @Text(value = 10, options = @IndexOptions(name = "single_annotation", languageOverride = "nativeTongue"))
        private String nickName;

    }

    @Entity
    @Indexes({@Index(fields = @Field(value = "name", type = TEXT)),
              @Index(fields = @Field(value = "nickName", type = TEXT))})
    private static class MultipleTextIndexes {
        @Id
        private ObjectId id;
        private String name;
        private String nickName;
    }
}
