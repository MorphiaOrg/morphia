package dev.morphia.test.query;

import com.mongodb.MongoException;
import dev.morphia.Datastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.query.FindOptions;
import dev.morphia.test.TestBase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;

public class TestMaxMin extends TestBase {

    @BeforeMethod
    public void setUp() {
        getMapper().map(IndexedEntity.class);
        getDs().ensureIndexes();
    }

    @Test(expectedExceptions = MongoException.class)
    public void testExceptionForIndexMismatch() {
        getDs().find(IndexedEntity.class).iterator(new FindOptions()
                                                       .limit(1)
                                                       .min(new Document("doesNotExist", 1)))
               .next();
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

        ds.ensureIndexes();
        Assert.assertEquals(ds.find(IndexedEntity.class)
                              .iterator(new FindOptions()
                                            .sort(descending("id"))
                                            .hint("testField")
                                            .max(new Document("testField", "c")))
                              .next().id,

            b.id, "last");
        Assert.assertEquals(ds.find(IndexedEntity.class)
                              .iterator(new FindOptions()
                                            .sort(descending("id"))
                                            .hint("testField")
                                            .max(new Document("testField", "c")))
                              .next().id,
            b.id, "last");
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

        List<IndexedEntity> l = ds.find(IndexedEntity.class).iterator(new FindOptions()
                                                                          .sort(ascending("testField"), ascending("id"))
                                                                          .hint(new Document("testField", 1)
                                                                                    .append("_id", 1))
                                                                          .max(new Document("testField", "b").append("_id", b2.id)))
                                  .toList();

        Assert.assertEquals(l.size(), 3, "size");
        Assert.assertEquals(l.get(2).id, b1.id, "item");

        l = ds.find(IndexedEntity.class).iterator(new FindOptions()
                                                      .sort(ascending("testField"), ascending("id"))
                                                      .hint(new Document("testField", 1)
                                                                .append("_id", 1))
                                                      .max(new Document("testField", "b").append("_id", b2.id)))
              .toList();

        Assert.assertEquals(l.size(), 3, "size");
        Assert.assertEquals(l.get(2).id, b1.id, "item");
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

        Assert.assertEquals(ds.find(IndexedEntity.class)
                              .iterator(new FindOptions()
                                            .sort(ascending("id"))
                                            .hint("testField")
                                            .min(new Document("testField", "b")))
                              .next().id,
            b.id, "last");

        Assert.assertEquals(ds.find(IndexedEntity.class)
                              .iterator(new FindOptions()
                                            .sort(ascending("id"))
                                            .hint("testField")
                                            .min(new Document("testField", "b")))
                              .next().id,
            b.id, "last");
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

        List<IndexedEntity> l = ds.find(IndexedEntity.class).iterator(new FindOptions()
                                                                          .sort(ascending("testField"), ascending("id"))
                                                                          .hint(new Document("testField", 1)
                                                                                    .append("_id", 1))
                                                                          .min(new Document("testField", "b").append("_id", b1.id)))
                                  .toList();

        Assert.assertEquals(l.size(), 4, "size");
        Assert.assertEquals(l.get(0).id, b1.id, "item");

        l = ds.find(IndexedEntity.class).iterator(new FindOptions()
                                                      .sort(ascending("testField"), ascending("id"))
                                                      .hint(new Document("testField", 1)
                                                                .append("_id", 1))
                                                      .min(new Document("testField", "b").append("_id", b1.id)))
              .toList();

        Assert.assertEquals(l.size(), 4, "item");
        Assert.assertEquals(l.get(0).id, b1.id, "item");
    }

    @Entity("IndexedEntity")
    @Indexes({
        @Index(fields = @Field("testField"),
            options = @IndexOptions(name = "testField")),
        @Index(fields = {@Field("testField"), @Field("_id")})})
    private static final class IndexedEntity {

        @Id
        private ObjectId id;
        private String testField;

        private IndexedEntity(String testField) {
            this.testField = testField;
        }

        private IndexedEntity() {
        }
    }
}
