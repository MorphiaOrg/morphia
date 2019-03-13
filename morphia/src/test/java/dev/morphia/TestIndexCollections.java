package dev.morphia;


import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Property;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static dev.morphia.utils.IndexType.DESC;


public class TestIndexCollections extends TestBase {

    @Test
    public void testEmbedded() {
        AdvancedDatastore ads = getAds();
        DB db = getDb();
        getMorphia().map(HasEmbeddedIndex.class);
        ads.ensureIndexes();

        ads.ensureIndexes("b_2", HasEmbeddedIndex.class);
        DBObject[] indexes = new BasicDBObject[]{
            new BasicDBObject("name", 1),
            new BasicDBObject("embeddedIndex.color", -1),
            new BasicDBObject("embeddedIndex.name", 1),
        };

        testIndex(db.getCollection("b_2").getIndexInfo(), indexes);
        testIndex(ads.getCollection(HasEmbeddedIndex.class).getIndexInfo(), indexes);
    }

    @Test
    public void testDisablingEmbedded() {
        AdvancedDatastore ads = getAds();
        ads.getMapper().getOptions().setDisableEmbeddedIndexes(true);
        getDs().getCollection(HasEmbeddedIndex.class).drop();
        getMorphia().map(HasEmbeddedIndex.class);
        ads.ensureIndexes();

        DBObject[] indexes = new BasicDBObject[]{
            new BasicDBObject("name", 1),
        };

        testIndex(ads.getCollection(HasEmbeddedIndex.class).getIndexInfo(), indexes);
    }

    @Test
    public void testOldStyleIndexing() {
        getMorphia().map(OldStyleIndexing.class);
        getDb().dropDatabase();
        getAds().ensureIndexes();
        testIndex(getAds().getCollection(OldStyleIndexing.class).getIndexInfo(),
                  new BasicDBObject("field", 1),
                  new BasicDBObject("field2", -1),
                  new BasicDBObject("f3", 1));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSingleFieldIndex() {
        AdvancedDatastore ads = getAds();
        DB db = getDb();

        ads.ensureIndexes("a_1", SingleFieldIndex.class);
        testIndex(db.getCollection("a_1").getIndexInfo(),
                  new BasicDBObject("field", 1),
                  new BasicDBObject("field2", -1),
                  new BasicDBObject("f3", 1));

        ads.ensureIndex("a_2", SingleFieldIndex.class, "-field2");
        ads.ensureIndexes("a_2", SingleFieldIndex.class);
        testIndex(db.getCollection("a_2").getIndexInfo(),
                  new BasicDBObject("field", 1),
                  new BasicDBObject("field2", 1),
                  new BasicDBObject("field2", -1),
                  new BasicDBObject("f3", 1));


        ads.ensureIndex("a_3", SingleFieldIndex.class, "field, field2");
        testIndex(db.getCollection("a_3").getIndexInfo(), new BasicDBObject("field", 1)
            .append("field2", 1));
    }

    private void testIndex(final List<DBObject> indexInfo, final DBObject... indexes) {
        Set<String> expected = new TreeSet<String>();
        Set<String> actual = new TreeSet<String>();
        for (final DBObject index : indexes) {
            expected.addAll(index.keySet());
        }

        for (final DBObject dbObject : indexInfo) {
            if (!"_id_".equals(dbObject.get("name"))) {
                actual.addAll(((DBObject) dbObject.get("key")).keySet());
            }
        }
        Assert.assertEquals("Should have found all the indexes.", expected, actual);
    }

    @Entity
    @Indexes({@Index(fields = @Field(value = "field2", type = DESC)), @Index(fields = @Field("field3"))})
    private static class SingleFieldIndex {
        @Id
        private ObjectId id;
        @Indexed
        private String field;
        @Property
        private String field2;
        @Property("f3")
        private String field3;
    }

    @Entity
    @Indexes({@Index(fields = @Field(value = "field2", type = DESC)), @Index(fields = @Field("field3"))})
    private static class OldStyleIndexing {
        @Id
        private ObjectId id;
        @Indexed
        private String field;
        @Property
        private String field2;
        @Property("f3")
        private String field3;
    }

    @Entity
    private static class HasEmbeddedIndex {
        @Id
        private ObjectId id;
        @Indexed
        private String name;
        @Embedded
        private EmbeddedIndex embeddedIndex;
    }

    @Embedded
    @Indexes(@Index(fields = @Field(value = "color", type = DESC)))
    private static class EmbeddedIndex {
        @Indexed
        private String name;
        private String color;
    }
}
