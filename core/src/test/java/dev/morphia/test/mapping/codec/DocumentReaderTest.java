package dev.morphia.test.mapping.codec;

import com.mongodb.client.MongoCollection;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PreLoad;
import dev.morphia.mapping.codec.reader.DocumentReader;
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
        step(r -> assertEquals(r.getCurrentBsonType(), BsonType.DOCUMENT));
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
    public void testByteArray() {
        Document document = parse("{ '_id': { '$oid': '59580ebf36218c7edf155044' }, " +
                                  "'data': { '$binary': { 'base64': '', 'subType': '00' } } }");
        getDs().getMapper().fromDocument(HasByteArray.class, document);
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
