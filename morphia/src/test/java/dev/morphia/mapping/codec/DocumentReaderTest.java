package dev.morphia.mapping.codec;

import com.mongodb.client.MongoCollection;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PreLoad;
import dev.morphia.mapping.codec.reader.DocumentReader;
import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static dev.morphia.query.experimental.filters.Filters.eq;

public class DocumentReaderTest extends TestBase {

    private DocumentReader reader;

    @Test
    public void mark() {
        setup(new Document("key", "value")
                  .append("nested", "detsen"));

        step(r -> {
            r.readStartDocument();
        });
        BsonReaderMark docMark = reader.getMark();

        step(r -> {
            Assert.assertEquals("key", r.readName());
        });
        step(r -> {
            Assert.assertEquals("value", r.readString());
        });

        docMark.reset();

        step(r -> {
            Assert.assertEquals("key", r.readName());
        });
        step(r -> {
            Assert.assertEquals("value", r.readString());
        });

        step(r -> {
            Assert.assertEquals("nested", r.readName());
        });
        step(r -> {
            Assert.assertEquals("detsen", r.readString());
        });

        step(r -> {
            r.readEndDocument();
        });
    }

    @Test
    public void nested() {
        setup(new Document("key", new Document("nested", "detsen"))
                  .append("list", List.of(
                      new Document("list1", "value1"),
                      new Document("list2", "value2"),
                      new Document("list3", "value3")
                                         )));

        step(r -> {
            r.readStartDocument();
        });
        step(r -> {
            Assert.assertEquals(BsonType.DOCUMENT, r.getCurrentBsonType());
        });
        step(r -> {
            Assert.assertEquals("key", r.readName());
        });
        step(r -> {
            r.readStartDocument();
        });
        step(r -> {
            Assert.assertEquals("nested", r.readName());
        });
        step(r -> {
            Assert.assertEquals("detsen", r.readString());
        });
        step(r -> {
            r.readEndDocument();
        });
        step(r -> {
            Assert.assertEquals("list", r.readName());
        });
        step(r -> {
            r.readStartArray();
        });
        readDocument(1);
        readDocument(2);
        readDocument(3);

        step(r -> {
            r.readEndArray();
        });
        step(r -> {
            r.readEndDocument();
        });

        step(r -> {
            r.close();
        });
    }

    @Test
    public void nestedDatabaseRead() {
        getDs().getMapper().map(Parent.class, Child.class);
        Parent parent = new Parent();
        parent.child = new Child();
        getDs().save(parent);

        MongoCollection<Document> collection = getDocumentCollection(Parent.class);

        Document first = collection.find().first();

        Parent decode = getMapper().getCodecRegistry().get(Parent.class)
                                   .decode(new DocumentReader(first), DecoderContext.builder().build());

        Assert.assertEquals(parent, decode);
    }

    @Test
    public void read() {
        setup(new Document("key", "value")
                  .append("numbers", List.of(1, 2, 3, 4, 5))
                  .append("another", "entry"));

        step(r -> {
            r.readStartDocument();
        });
        step(r -> {
            Assert.assertEquals(BsonType.DOCUMENT, r.getCurrentBsonType());
        });
        step(r -> {
            Assert.assertEquals("key", r.readName());
        });
        step(r -> {
            Assert.assertEquals("value", r.readString());
        });
        step(r -> {
            Assert.assertEquals("numbers", r.readName());
        });
        step(r -> {
            r.readStartArray();
        });
        for (int i = 1; i < 6; i++) {
            final int finalI = i;
            step(r -> {
                Assert.assertEquals(finalI, r.readInt32());
            });
        }
        step(r -> {
            r.readEndArray();
        });
        step(r -> {
            Assert.assertEquals("another", r.readName());
        });
        step(r -> {
            Assert.assertEquals("entry", r.readString());
        });
        step(r -> {
            r.readEndDocument();
        });
    }

    @Test
    public void testDates() {
        final TimeEntity entity = new TimeEntity();
        entity.myInstant = Instant.now();
        final TimeEntity save = getDs().save(entity);
        final TimeEntity find = getDs().find(TimeEntity.class)
                                       .filter(eq("_id", save.id))
                                       .first();
    }

    private void readDocument(final int count) {
        step(r -> {
            r.readStartDocument();
        });
        step(r -> {
            Assert.assertEquals("list" + count, r.readName());
        });
        step(r -> {
            Assert.assertEquals("value" + count, r.readString());
        });
        step(r -> {
            r.readEndDocument();
        });
    }

    private void setup(final Document document) {
        reader = new DocumentReader(document);
    }

    private void step(final Consumer<BsonReader> function) {
        function.accept(reader);
    }

    @Embedded
    private static class Child {
        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(final Object o) {
            return this == o || o instanceof Child;
        }
    }

    @Entity
    private static class Parent {
        @Id
        private ObjectId id;
        private Child child;

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (child != null ? child.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Parent)) {
                return false;
            }

            final Parent parent = (Parent) o;

            if (id != null ? !id.equals(parent.id) : parent.id != null) {
                return false;
            }
            return child != null ? child.equals(parent.child) : parent.child == null;
        }
    }

    @Entity
    private static class TimeEntity {
        @Id
        public ObjectId id;
        public Instant myInstant;

        @PreLoad
        public void preload() {
        }
    }
}