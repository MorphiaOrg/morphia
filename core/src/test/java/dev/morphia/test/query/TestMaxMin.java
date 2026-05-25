package dev.morphia.test.query;

import java.util.List;

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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;

public class TestMaxMin extends TestBase {

    @BeforeEach
    public void setUp() {
        getMapper().map(IndexedEntity.class);
        getDs().applyIndexes();
    }

    @Test
    public void testExceptionForIndexMismatch() {
        Assertions.assertThrows(MongoException.class, () -> {
            getDs().find(IndexedEntity.class,
                    new FindOptions()
                            .limit(1)
                            .min(new Document("doesNotExist", 1)))
                    .iterator()
                    .next();
        });
    }

    @Test
    public void testMax() {
        final IndexedEntity a = new IndexedEntity("a");
        final IndexedEntity b = new IndexedEntity("b");
        final IndexedEntity c = new IndexedEntity("c");

        var ds = getDs();

        ds.save(a);
        ds.save(b);
        ds.save(c);

        ds.applyIndexes();
        Assertions.assertEquals(b.id, ds.find(IndexedEntity.class,
                new FindOptions()
                        .sort(descending("id"))
                        .hint("testField")
                        .max(new Document("testField", "c")))
                .iterator()
                .next().id, "last");
        Assertions.assertEquals(b.id, ds.find(IndexedEntity.class,
                new FindOptions()
                        .sort(descending("id"))
                        .hint("testField")
                        .max(new Document("testField", "c")))
                .iterator()
                .next().id, "last");
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

        List<IndexedEntity> l = ds.find(IndexedEntity.class,
                new FindOptions()
                        .sort(ascending("testField"), ascending("id"))
                        .hint(new Document("testField", 1)
                                .append("_id", 1))
                        .max(new Document("testField", "b").append("_id", b2.id)))
                .iterator()
                .toList();

        Assertions.assertEquals(3, l.size(), "size");
        Assertions.assertEquals(b1.id, l.get(2).id, "item");

        l = ds.find(IndexedEntity.class,
                new FindOptions()
                        .sort(ascending("testField"), ascending("id"))
                        .hint(new Document("testField", 1)
                                .append("_id", 1))
                        .max(new Document("testField", "b").append("_id", b2.id)))
                .iterator()
                .toList();

        Assertions.assertEquals(3, l.size(), "size");
        Assertions.assertEquals(b1.id, l.get(2).id, "item");
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

        Assertions.assertEquals(b.id, ds.find(IndexedEntity.class,
                new FindOptions()
                        .sort(ascending("id"))
                        .hint("testField")
                        .min(new Document("testField", "b")))
                .iterator()
                .next().id, "last");

        Assertions.assertEquals(b.id, ds.find(IndexedEntity.class,
                new FindOptions()
                        .sort(ascending("id"))
                        .hint("testField")
                        .min(new Document("testField", "b")))
                .iterator()
                .next().id, "last");
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

        List<IndexedEntity> l = ds.find(IndexedEntity.class,
                new FindOptions()
                        .sort(ascending("testField"), ascending("id"))
                        .hint(new Document("testField", 1)
                                .append("_id", 1))
                        .min(new Document("testField", "b").append("_id", b1.id)))
                .iterator()
                .toList();

        Assertions.assertEquals(4, l.size(), "size");
        Assertions.assertEquals(b1.id, l.get(0).id, "item");

        l = ds.find(IndexedEntity.class,
                new FindOptions()
                        .sort(ascending("testField"), ascending("id"))
                        .hint(new Document("testField", 1)
                                .append("_id", 1))
                        .min(new Document("testField", "b").append("_id", b1.id)))
                .iterator()
                .toList();

        Assertions.assertEquals(4, l.size(), "item");
        Assertions.assertEquals(b1.id, l.get(0).id, "item");
    }

    @Entity("IndexedEntity")
    @Indexes({
            @Index(fields = @Field("testField"), options = @IndexOptions(name = "testField")),
            @Index(fields = { @Field("testField"), @Field("_id") }) })
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
