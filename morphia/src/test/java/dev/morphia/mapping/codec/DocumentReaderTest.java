package dev.morphia.mapping.codec;

import com.mongodb.client.MongoCollection;
import dev.morphia.TestBase;
import dev.morphia.TestUpdateOps.Log;
import dev.morphia.TestUpdateOps.LogHolder;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.codec.reader.FlattenedDocumentReader;
import org.bson.BsonDocumentReader;
import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.function.Consumer;

public class DocumentReaderTest extends TestBase {

    private FlattenedDocumentReader reader;
    private BsonDocumentReader bsonDocumentReader;

    @Test
    public void read() {
        setup(new Document("key", "value")
                  .append("numbers", List.of(1, 2, 3, 4, 5))
                  .append("another", "entry"));

        step(r -> { r.readStartDocument();});
        step(r -> { Assert.assertEquals(BsonType.DOCUMENT, r.getCurrentBsonType());});
        step(r -> { Assert.assertEquals("key", r.readName());});
        step(r -> { Assert.assertEquals("value", r.readString());});
        step(r -> { Assert.assertEquals("numbers", r.readName());});
        step(r -> { r.readStartArray();});
        for (int i = 1; i < 6; i++) {
            final int finalI = i;
            step(r -> { Assert.assertEquals(finalI, r.readInt32());});
        }
        step(r -> { r.readEndArray();});
        step(r -> { Assert.assertEquals("another", r.readName());});
        step(r -> { Assert.assertEquals("entry", r.readString());});
        step(r -> { r.readEndDocument();});
    }

    @Test
    public void nested() {
        setup(new Document("key", new Document("nested", "detsen"))
             .append("list", List.of(
                 new Document("list1", "value1"),
                 new Document("list2", "value2"),
                 new Document("list3", "value3")
                                    )));

        step(r -> { r.readStartDocument();});
        step(r -> { Assert.assertEquals(BsonType.DOCUMENT, r.getCurrentBsonType());});
        step(r -> { Assert.assertEquals("key", r.readName());});
        step(r -> { r.readStartDocument();});
        step(r -> { Assert.assertEquals("nested", r.readName());});
        step(r -> { Assert.assertEquals("detsen", r.readString());});
        step(r -> { r.readEndDocument();});
        step(r -> { Assert.assertEquals("list", r.readName());});
        step(r -> { r.readStartArray();});
        readDocument(1);
        readDocument(2);
        readDocument(3);

        step(r -> { r.readEndArray();});
        step(r -> { r.readEndDocument();});

        step(r -> { r.close();});
    }

    private void readDocument(final int count) {
        step(r -> { r.readStartDocument();});
        step(r -> { Assert.assertEquals("list" + count, r.readName());});
        step(r -> { Assert.assertEquals("value" + count, r.readString());});
        step(r -> { r.readEndDocument();});
    }

    @Test
    public void mark() {
        setup(new Document("key", "value")
                  .append("nested", "detsen"));

        step(r -> { r.readStartDocument();});
        BsonReaderMark bsonMark = bsonDocumentReader.getMark();
        BsonReaderMark docMark = reader.getMark();

        step(r -> { Assert.assertEquals("key", r.readName());});
        step(r -> { Assert.assertEquals("value", r.readString());});

        bsonMark.reset();
        docMark.reset();

        step(r -> { Assert.assertEquals("key", r.readName());});
        step(r -> { Assert.assertEquals("value", r.readString());});

        step(r -> { Assert.assertEquals("nested", r.readName());});
        step(r -> { Assert.assertEquals("detsen", r.readString());});

        step(r -> { r.readEndDocument();});
    }

    @Test
    public void nestedDatabaseRead() {
        getDs().getMapper().map(Parent.class, Child.class);
        Parent parent = new Parent();
        parent.child = new Child();
        getDs().save(parent);

        MongoCollection<Document> collection = getDatabase()
                                                   .getCollection(Parent.class.getSimpleName());

        Document first = collection.find().first();

        Parent decode = getMapper().getCodecRegistry().get(Parent.class)
                                      .decode(new FlattenedDocumentReader(first), DecoderContext.builder().build());

        Assert.assertEquals(parent, decode);
    }

    @Test
    public void databaseRead() {
        getDs().getMapper().map(LogHolder.class, Log.class);
        LogHolder holder = new LogHolder();
        holder.setLog(new Log(1));
        getDs().save(holder);
        MongoCollection<Document> collection = getDatabase()
                                                   .getCollection(LogHolder.class.getSimpleName());

        LogHolder decode = getMapper().getCodecRegistry().get(LogHolder.class)
                                      .decode(new FlattenedDocumentReader(collection.find().first()), DecoderContext.builder().build());

        Assert.assertEquals(holder, decode);
    }

    private void setup(final Document document) {
        reader = new FlattenedDocumentReader(document);
        bsonDocumentReader = new BsonDocumentReader(
            document.toBsonDocument(Document.class, getMapper().getCodecRegistry()));
    }

    private void step(final Consumer<BsonReader> function) {
//        function.accept(bsonDocumentReader);
        function.accept(reader);
    }

    @Entity
    private static class Parent {
        @Id
        private ObjectId id;
        private Child child;

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

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (child != null ? child.hashCode() : 0);
            return result;
        }
    }

    @Embedded
    private static class Child {
        @Override
        public boolean equals(final Object o) {
            return this == o || o instanceof Child;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }
}