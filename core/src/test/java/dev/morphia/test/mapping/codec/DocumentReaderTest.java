package dev.morphia.test.mapping.codec;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.mongodb.client.MongoCollection;

import dev.morphia.EntityListener;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PreLoad;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.mapping.codec.reader.NameState;
import dev.morphia.mapping.codec.reader.ValueState;
import dev.morphia.test.TestBase;

import org.bson.BsonBinary;
import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static org.bson.Document.parse;

public class DocumentReaderTest extends TestBase {

    private DocumentReader reader;

    @Test
    public void mark() {
        setup(new Document("key", "value")
                .append("nested", "detsen"));

        step(BsonReader::readStartDocument);
        BsonReaderMark docMark = reader.getMark();

        step(r -> Assertions.assertEquals("key", r.readName()));
        step(r -> Assertions.assertEquals("value", r.readString()));

        docMark.reset();

        step(r -> Assertions.assertEquals("key", r.readName()));
        step(r -> Assertions.assertEquals("value", r.readString()));

        step(r -> Assertions.assertEquals("nested", r.readName()));
        step(r -> Assertions.assertEquals("detsen", r.readString()));

        step(BsonReader::readEndDocument);
    }

    @Test
    public void nested() {
        setup(new Document("key", new Document("nested", "detsen"))
                .append("list", List.of(
                        new Document("list1", "value1"),
                        new Document("list2", "value2"),
                        new Document("list3", "value3"))));

        step(BsonReader::readStartDocument);
        step(r -> Assertions.assertEquals(BsonType.DOCUMENT, r.getCurrentBsonType()));
        step(r -> Assertions.assertEquals("key", r.readName()));
        step(BsonReader::readStartDocument);
        step(r -> Assertions.assertEquals("nested", r.readName()));
        step(r -> Assertions.assertEquals("detsen", r.readString()));
        step(BsonReader::readEndDocument);
        step(r -> Assertions.assertEquals("list", r.readName()));
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

        Parent decode = getDs().getCodecRegistry().get(Parent.class)
                .decode(new DocumentReader(first, getDs().getMapper().getConversions()), DecoderContext.builder().build());

        Assertions.assertEquals(decode, parent);
    }

    @Test
    public void read() {
        setup(new Document("key", "value")
                .append("numbers", List.of(1, 2, 3, 4, 5))
                .append("another", "entry"));

        step(BsonReader::readStartDocument);
        step(r -> Assertions.assertEquals(BsonType.STRING, r.getCurrentBsonType()));
        step(r -> Assertions.assertEquals("key", r.readName()));
        step(r -> Assertions.assertEquals("value", r.readString()));
        step(r -> Assertions.assertEquals("numbers", r.readName()));
        step(BsonReader::readStartArray);
        for (int i = 1; i < 6; i++) {
            final int finalI = i;
            step(r -> Assertions.assertEquals(r.readInt32(), finalI));
        }
        step(BsonReader::readEndArray);
        step(r -> Assertions.assertEquals("another", r.readName()));
        step(r -> Assertions.assertEquals("entry", r.readString()));
        step(BsonReader::readEndDocument);
    }

    @Test
    public void testBookmarkingAndSkips() {
        setup(Document.parse("{ key: { subKey: 3 }, second: 2 }"));

        step(r -> Assertions.assertEquals(BsonType.DOCUMENT, r.getCurrentBsonType()));
        step(BsonReader::readStartDocument);
        step(r -> Assertions.assertEquals("key", r.readName()));
        BsonReaderMark mark = reader.getMark();
        step(BsonReader::readStartDocument);
        step(r -> Assertions.assertEquals("subKey", r.readName()));
        step(r -> Assertions.assertEquals(3, r.readInt32()));
        step(BsonReader::readEndDocument);
        mark.reset();
        reader.skipValue();
        step(r -> Assertions.assertEquals("second", r.readName()));
        step(r -> Assertions.assertEquals(2, r.readInt32()));
        step(BsonReader::readEndDocument);

        setup(Document.parse("{ key: [ 1, 2, 3 ], second: 8 }"));

        step(r -> Assertions.assertEquals(BsonType.DOCUMENT, r.getCurrentBsonType()));
        step(BsonReader::readStartDocument);
        step(r -> Assertions.assertEquals("key", r.readName()));
        mark = reader.getMark();
        step(BsonReader::readStartArray);
        step(r -> Assertions.assertEquals(1, r.readInt32()));
        step(r -> Assertions.assertEquals(2, r.readInt32()));
        mark.reset();
        reader.skipValue();
        step(r -> Assertions.assertEquals("second", r.readName()));
        step(r -> Assertions.assertEquals(8, r.readInt32()));
        step(BsonReader::readEndDocument);
    }

    @Test
    public void testBsonBinary() {
        byte[] data = { 1, 2, 3, 4 };
        BsonBinary bsonBinary = new BsonBinary(data);

        setup(new Document("bin", bsonBinary));
        step(BsonReader::readStartDocument);
        step(r -> Assertions.assertEquals("bin", r.readName()));
        step(r -> Assertions.assertEquals(BsonType.BINARY, r.getCurrentBsonType()));
        step(r -> Assertions.assertEquals(bsonBinary, r.readBinaryData()));
        step(BsonReader::readEndDocument);

        Binary legacy = new Binary(data);
        setup(new Document("bin", legacy));
        step(BsonReader::readStartDocument);
        step(r -> Assertions.assertEquals("bin", r.readName()));
        step(r -> Assertions.assertEquals(BsonType.BINARY, r.getCurrentBsonType()));
        step(r -> Assertions.assertTrue(Arrays.equals(r.readBinaryData().getData(), data)));
        step(BsonReader::readEndDocument);
    }

    @Test
    public void testBsonBinaryRoundTrip() {
        HasBsonBinary entity = new HasBsonBinary();
        entity.data = new BsonBinary(new byte[] { 5, 10, 15 });
        getDs().save(entity);
        Document first = getDs().getCollection(HasBsonBinary.class)
                .withDocumentClass(Document.class)
                .find().first();
        HasBsonBinary decoded = fromDocument(HasBsonBinary.class, first);
        Assertions.assertEquals(entity.data, decoded.data);
    }

    @Test
    public void testByteArray() {
        HasByteArray hasByteArray = new HasByteArray();
        hasByteArray.data = new byte[] { 1, 2, 3 };
        getDs().save(hasByteArray);
        Document first = getDs().getCollection(HasByteArray.class)
                .withDocumentClass(Document.class)
                .find().first();
        fromDocument(HasByteArray.class, first);
    }

    @Test
    public void testNestedByteArray() {
        HasByteArray hasByteArray = new HasByteArray();
        hasByteArray.data = new byte[] { 1, 2, 3 };
        HasNestedByteArray hasNestedByteArray = new HasNestedByteArray();
        hasNestedByteArray.nested = hasByteArray;
        getDs().save(hasNestedByteArray);
        Document first = getDs().getCollection(HasNestedByteArray.class)
                .withDocumentClass(Document.class)
                .find().first();
        fromDocument(HasNestedByteArray.class, first);
    }

    @Test
    public void testNestedByteArrayWithInterceptor() {
        HasByteArray hasByteArray = new HasByteArray();
        hasByteArray.data = new byte[] { 1, 2, 3 };
        HasNestedByteArray hasNestedByteArray = new HasNestedByteArray();
        hasNestedByteArray.nested = hasByteArray;
        getDs().save(hasNestedByteArray);
        EntityListener<?> interceptor = (EntityListener) type -> false;
        try {
            getDs().getMapper().addInterceptor(interceptor);
            Document first = getDs().getCollection(HasNestedByteArray.class)
                    .withDocumentClass(Document.class)
                    .find().first();
            fromDocument(HasNestedByteArray.class, first);
        } finally {
            getDs().getMapper().getListeners().remove(interceptor);
        }
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

        step(r -> Assertions.assertEquals(BsonType.DOCUMENT, r.getCurrentBsonType()));
        step(BsonReader::readStartDocument);
        step(r -> Assertions.assertEquals("coordinates", r.readName()));

        step(r -> Assertions.assertEquals(BsonType.ARRAY, r.getCurrentBsonType()));
        step(BsonReader::readStartArray);

        step(r -> Assertions.assertEquals(BsonType.ARRAY, r.getCurrentBsonType()));
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

        step(r -> Assertions.assertEquals(BsonType.DOCUMENT, r.getCurrentBsonType()));
        step(BsonReader::readStartDocument);
        Assertions.assertTrue(reader.currentState() instanceof NameState);
        step(r -> {
            Assertions.assertEquals("key", r.readName());
            Assertions.assertTrue(reader.currentState() instanceof ValueState);
        });
        step(r -> {
            r.readNull();
            Assertions.assertTrue(reader.currentState() instanceof NameState);
        });
        step(r -> {
            Assertions.assertEquals("another", r.readName());
            Assertions.assertTrue(reader.currentState() instanceof ValueState);
        });
        step(r -> Assertions.assertEquals("fun", r.readString()));
        step(BsonReader::readEndDocument);
    }

    @Test
    public void testSkips() {
        setup(Document.parse("{ key: 'value', second: 2 }"));

        step(r -> {
            Assertions.assertEquals(BsonType.DOCUMENT, r.getCurrentBsonType());
            r.readStartDocument();
            Assertions.assertTrue(reader.currentState() instanceof NameState);
        });
        step(r -> {
            r.skipName();
            Assertions.assertTrue(reader.currentState() instanceof ValueState);
        });
        step(r -> {
            r.skipValue();
            Assertions.assertTrue(reader.currentState() instanceof NameState);
        });
    }

    private void readDocument(int count) {
        step(BsonReader::readStartDocument);
        step(r -> Assertions.assertEquals(r.readName(), "list" + count));
        step(r -> Assertions.assertEquals(r.readString(), "value" + count));
        step(BsonReader::readEndDocument);
    }

    private void setup(Document document) {
        reader = new DocumentReader(document, new dev.morphia.mapping.codec.Conversions(Thread.currentThread().getContextClassLoader()));
    }

    private void step(Consumer<BsonReader> function) {
        function.accept(reader);
    }

    private void testArray(int i) {
        step(r -> Assertions.assertEquals(BsonType.ARRAY, r.getCurrentBsonType()));
        step(BsonReader::readStartArray);
        step(r -> Assertions.assertEquals(BsonType.INT32, r.getCurrentBsonType()));
        int expected = i;
        step(r -> Assertions.assertEquals(expected, r.readInt32()));
        step(r -> Assertions.assertEquals(expected + 1, r.readInt32()));
        step(BsonReader::readEndArray);
    }

    @Entity
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
    private static class HasBsonBinary {
        @Id
        private ObjectId id;
        private BsonBinary data;
    }

    @Entity
    private static class HasByteArray {
        @Id
        private ObjectId id;
        private byte[] data;
    }

    @Entity
    private static class HasNestedByteArray {
        @Id
        private ObjectId id;
        private HasByteArray nested;
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
