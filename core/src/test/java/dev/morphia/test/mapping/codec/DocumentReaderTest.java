package dev.morphia.test.mapping.codec;

import com.mongodb.client.MongoCollection;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PreLoad;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.mapping.codec.reader.NameState;
import dev.morphia.mapping.codec.reader.ValueState;
import dev.morphia.test.TestBase;
import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DocumentReaderTest extends TestBase {

    private DocumentReader reader;

    @Test
    public void mark() {
        setup(new Document("key", "value")
                  .append("nested", "detsen"));

        step(BsonReader::readStartDocument);
        BsonReaderMark docMark = reader.getMark();

        step(r -> assertEquals(r.readName(), "key"));
        step(r -> assertEquals(r.readString(), "value"));

        docMark.reset();

        step(r -> assertEquals(r.readName(), "key"));
        step(r -> assertEquals(r.readString(), "value"));

        step(r -> assertEquals(r.readName(), "nested"));
        step(r -> assertEquals(r.readString(), "detsen"));

        step(BsonReader::readEndDocument);
    }

    @Test
    public void nested() {
        setup(new Document("key", new Document("nested", "detsen"))
                  .append("list", List.of(
                      new Document("list1", "value1"),
                      new Document("list2", "value2"),
                      new Document("list3", "value3")
                                         )));

        step(BsonReader::readStartDocument);
        step(r -> assertEquals(r.getCurrentBsonType(), BsonType.DOCUMENT));
        step(r -> assertEquals(r.readName(), "key"));
        step(BsonReader::readStartDocument);
        step(r -> assertEquals(r.readName(), "nested"));
        step(r -> assertEquals(r.readString(), "detsen"));
        step(BsonReader::readEndDocument);
        step(r -> assertEquals(r.readName(), "list"));
        step(BsonReader::readStartArray);
        readDocument(1);
        readDocument(2);
        readDocument(3);

        step(BsonReader::readEndArray);
        step(BsonReader::readEndDocument);

        step(BsonReader::close);
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

        assertEquals(parent, decode);
    }

    @Test
    public void read() {
        setup(new Document("key", "value")
                  .append("numbers", List.of(1, 2, 3, 4, 5))
                  .append("another", "entry"));

        step(BsonReader::readStartDocument);
        step(r -> assertEquals(r.getCurrentBsonType(), BsonType.STRING));
        step(r -> assertEquals(r.readName(), "key"));
        step(r -> assertEquals(r.readString(), "value"));
        step(r -> assertEquals(r.readName(), "numbers"));
        step(BsonReader::readStartArray);
        for (int i = 1; i < 6; i++) {
            final int finalI = i;
            step(r -> assertEquals(finalI, r.readInt32()));
        }
        step(BsonReader::readEndArray);
        step(r -> assertEquals(r.readName(), "another"));
        step(r -> assertEquals(r.readString(), "entry"));
        step(BsonReader::readEndDocument);
    }

    @Test
    public void testBookmarkingAndSkips() {
        setup(Document.parse("{ key: { subKey: 3 }, second: 2 }"));

        step(r -> assertEquals(r.getCurrentBsonType(), BsonType.DOCUMENT));
        step(BsonReader::readStartDocument);
        step(r -> assertEquals(r.readName(), "key"));
        BsonReaderMark mark = reader.getMark();
        step(BsonReader::readStartDocument);
        step(r -> assertEquals(r.readName(), "subKey"));
        step(r -> assertEquals(r.readInt32(), 3));
        step(BsonReader::readEndDocument);
        mark.reset();
        reader.skipValue();
        step(r -> assertEquals(r.readName(), "second"));
        step(r -> assertEquals(r.readInt32(), 2));
        step(BsonReader::readEndDocument);

        setup(Document.parse("{ key: [ 1, 2, 3 ], second: 8 }"));

        step(r -> assertEquals(r.getCurrentBsonType(), BsonType.DOCUMENT));
        step(BsonReader::readStartDocument);
        step(r -> assertEquals(r.readName(), "key"));
        mark = reader.getMark();
        step(BsonReader::readStartArray);
        step(r -> assertEquals(r.readInt32(), 1));
        step(r -> assertEquals(r.readInt32(), 2));
        mark.reset();
        reader.skipValue();
        step(r -> assertEquals(r.readName(), "second"));
        step(r -> assertEquals(r.readInt32(), 8));
        step(BsonReader::readEndDocument);
    }

    @Test
    public void testByteArray() {
        HasByteArray hasByteArray = new HasByteArray();
        hasByteArray.data = new byte[]{1, 2, 3};
        getDs().save(hasByteArray);
        Document first = getDs().getMapper().getCollection(HasByteArray.class)
                                .withDocumentClass(Document.class)
                                .find().first();
        getDs().getMapper().fromDocument(HasByteArray.class, first);
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

    @Test
    public void testNestedArrays() {
        setup(parse("{ coordinates : [ [ [ 0, 1 ], [ 1, 2 ], [ 2, 3 ], [ 3, 4 ], [ 4, 5 ] ] ] }"));

        step(r -> assertEquals(r.getCurrentBsonType(), BsonType.DOCUMENT));
        step(BsonReader::readStartDocument);
        step(r -> assertEquals(r.readName(), "coordinates"));

        step(r -> assertEquals(r.getCurrentBsonType(), BsonType.ARRAY));
        step(BsonReader::readStartArray);

        step(r -> assertEquals(r.getCurrentBsonType(), BsonType.ARRAY));
        step(BsonReader::readStartArray);

        for (int i = 0; i < 5; i++) {
            testArray(i);
        }
        step(BsonReader::readEndArray);
        step(BsonReader::readEndArray);


        step(BsonReader::readEndDocument);
    }

    @Test
    public void testNulls() {
        setup(Document.parse("{ key: null, another: 'fun' }"));

        step(r -> assertEquals(r.getCurrentBsonType(), BsonType.DOCUMENT));
        step(BsonReader::readStartDocument);
        assertTrue(reader.currentState() instanceof NameState);
        step(r -> {
            assertEquals(r.readName(), "key");
            assertTrue(reader.currentState() instanceof ValueState);
        });
        step(r -> {
            r.readNull();
            assertTrue(reader.currentState() instanceof NameState);
        });
        step(r -> {
            assertEquals(r.readName(), "another");
            assertTrue(reader.currentState() instanceof ValueState);
        });
        step(r -> assertEquals(r.readString(), "fun"));
        step(BsonReader::readEndDocument);
    }

    @Test
    public void testSkips() {
        setup(Document.parse("{ key: 'value', second: 2 }"));

        step(r -> assertEquals(r.getCurrentBsonType(), BsonType.DOCUMENT));
        step(BsonReader::readStartDocument);
        assertTrue(reader.currentState() instanceof NameState);
        step(r -> {
            r.skipName();
            assertTrue(reader.currentState() instanceof ValueState);
        });
        step(r -> {
            r.skipValue();
            assertTrue(reader.currentState() instanceof NameState);
        });
    }

    private void readDocument(int count) {
        step(BsonReader::readStartDocument);
        step(r -> assertEquals("list" + count, r.readName()));
        step(r -> assertEquals("value" + count, r.readString()));
        step(BsonReader::readEndDocument);
    }

    private void setup(Document document) {
        reader = new DocumentReader(document);
    }

    private void step(Consumer<BsonReader> function) {
        function.accept(reader);
    }

    private void testArray(int i) {
        step(r -> assertEquals(r.getCurrentBsonType(), BsonType.ARRAY));
        step(BsonReader::readStartArray);
        step(r -> assertEquals(r.getCurrentBsonType(), BsonType.INT32));
        int expected = i;
        step(r -> assertEquals(r.readInt32(), expected));
        step(r -> assertEquals(r.readInt32(), expected + 1));
        step(BsonReader::readEndArray);
    }

    @Embedded
    private static class Child {
        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Child;
        }
    }

    @Entity
    private static class HasByteArray {
        @Id
        private ObjectId id;
        private byte[] data;
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
        public boolean equals(Object o) {
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
