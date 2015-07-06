package org.mongodb.morphia.query;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

import java.util.List;

public class TestMaxMin extends TestBase {

    @Override
    @Before
    public void setUp() {
        super.setUp();
        getMorphia().map(IndexedEntity.class);
        getDs().ensureIndexes();
    }

    @Test(expected = MongoException.class)
    public void testExceptionForIndexMismatch() throws Exception {
        getDs().createQuery(IndexedEntity.class).lowerIndexBound(new BasicDBObject("doesNotExist", 1)).get();
    }

    @Test
    public void testMax() {
        final IndexedEntity a = new IndexedEntity("a");
        final IndexedEntity b = new IndexedEntity("b");
        final IndexedEntity c = new IndexedEntity("c");

        Datastore ds = getDs();

        ds.save(a);
        ds.save(b);
        ds.save(c);

        Assert.assertEquals("last", b.id, ds.createQuery(IndexedEntity.class).order("-id")
                                            .upperIndexBound(new BasicDBObject("testField", "c")).get().id);
    }

    @Test
    public void testMaxCompoundIndex() {
        final IndexedEntity a1 = new IndexedEntity("a");
        final IndexedEntity a2 = new IndexedEntity("a");
        final IndexedEntity b1 = new IndexedEntity("b");
        final IndexedEntity b2 = new IndexedEntity("b");
        final IndexedEntity c1 = new IndexedEntity("c");
        final IndexedEntity c2 = new IndexedEntity("c");

        Datastore ds = getDs();

        ds.save(a1);
        ds.save(a2);
        ds.save(b1);
        ds.save(b2);
        ds.save(c1);
        ds.save(c2);

        List<IndexedEntity> l = ds.createQuery(IndexedEntity.class).order("testField, id")
                                  .upperIndexBound(new BasicDBObject("testField",
                                                                     "b").append("_id", b2.id)).asList();

        Assert.assertEquals("size", 3, l.size());
        Assert.assertEquals("item", b1.id, l.get(2).id);
    }

    @Test
    public void testMin() {
        final IndexedEntity a = new IndexedEntity("a");
        final IndexedEntity b = new IndexedEntity("b");
        final IndexedEntity c = new IndexedEntity("c");

        Datastore ds = getDs();

        ds.save(a);
        ds.save(b);
        ds.save(c);

        Assert.assertEquals("last", b.id, ds.createQuery(IndexedEntity.class).order("id")
                                            .lowerIndexBound(new BasicDBObject("testField", "b")).get().id);
    }

    @Test
    public void testMinCompoundIndex() {
        final IndexedEntity a1 = new IndexedEntity("a");
        final IndexedEntity a2 = new IndexedEntity("a");
        final IndexedEntity b1 = new IndexedEntity("b");
        final IndexedEntity b2 = new IndexedEntity("b");
        final IndexedEntity c1 = new IndexedEntity("c");
        final IndexedEntity c2 = new IndexedEntity("c");

        Datastore ds = getDs();

        ds.save(a1);
        ds.save(a2);
        ds.save(b1);
        ds.save(b2);
        ds.save(c1);
        ds.save(c2);

        Query<IndexedEntity> min = ds.createQuery(IndexedEntity.class).order("testField, id")
                                     .lowerIndexBound(new BasicDBObject("testField", "b").append("_id", b1.id));
        List<IndexedEntity> l = min.asList();

        Assert.assertEquals("size", 4, l.size());
        Assert.assertEquals("item", b1.id, l.get(0).id);
    }

    @Entity("IndexedEntity")
    @Indexes({
                 @Index(fields = @Field("testField")),
                 @Index(fields = {@Field("testField"), @Field("_id")})})
    private static final class IndexedEntity {

        @Id
        private ObjectId id;
        private String testField;

        private IndexedEntity(final String testField) {
            this.testField = testField;
        }

        private IndexedEntity() {
        }
    }
}
