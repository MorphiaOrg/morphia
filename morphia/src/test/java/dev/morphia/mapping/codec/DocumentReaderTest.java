package dev.morphia.mapping.codec;

import com.mongodb.client.MongoCollection;
import dev.morphia.TestBase;
import dev.morphia.TestUpdateOps.Log;
import dev.morphia.TestUpdateOps.LogHolder;
import org.bson.BsonDocumentReader;
import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.function.Consumer;

public class DocumentReaderTest extends TestBase {

    private DocumentReader reader;
    private BsonDocumentReader bsonDocumentReader;

    @Test
    public void read() {
        setup(new Document("key", "value")
            .append("numbers", List.of(1,2,3,4,5)));

        doIt(r -> { r.readStartDocument();});
        doIt(r -> { Assert.assertEquals(r.getCurrentBsonType(), BsonType.DOCUMENT);});
        doIt(r -> { Assert.assertEquals(r.readName(), "key");});
        doIt(r -> { Assert.assertEquals(r.readString(), "value");});
        doIt(r -> { Assert.assertEquals(r.readName(), "numbers");});
        doIt(r -> { r.readStartArray();});
        for (int i = 1; i < 6; i++) {
            final int finalI = i;
            doIt(r -> { Assert.assertEquals(r.readInt32(), finalI);});
        }
        doIt(r -> { r.readEndArray();});
        doIt(r -> { r.readEndDocument();});
    }

    @Test
    public void nested() {
        setup(new Document("key", new Document("nested", "detsen"))
             .append("list", List.of(
                 new Document("list1", "value1"),
                 new Document("list2", "value2"),
                 new Document("list3", "value3")
                                    )));

        doIt(r -> { r.readStartDocument();});
        doIt(r -> { Assert.assertEquals(r.getCurrentBsonType(), BsonType.DOCUMENT);});
        doIt(r -> { Assert.assertEquals(r.readName(), "key");});
        doIt(r -> { r.readStartDocument();});
        doIt(r -> { Assert.assertEquals(r.readName(), "nested");});
        doIt(r -> { Assert.assertEquals(r.readString(), "detsen");});
        doIt(r -> { r.readEndDocument();});
        doIt(r -> { Assert.assertEquals(r.readName(), "list");});
        doIt(r -> { r.readStartArray();});
        readDocument(1);
        readDocument(2);
        readDocument(3);

        doIt(r -> { r.readEndArray();});
        doIt(r -> { r.readEndDocument();});

        doIt(r -> { r.close();});
    }

    private void readDocument(final int count) {
        doIt(r -> { r.readStartDocument();});
        doIt(r -> { Assert.assertEquals(r.readName(), "list" + count);});
        doIt(r -> { Assert.assertEquals(r.readString(), "value" + count);});
        doIt(r -> { r.readEndDocument();});
    }

    @Test
    public void mark() {
        setup(new Document("key", "value")
                  .append("nested", "detsen"));

        doIt(r -> { r.readStartDocument();});
        BsonReaderMark bsonMark = bsonDocumentReader.getMark();
        BsonReaderMark docMark = reader.getMark();

        doIt(r -> { Assert.assertEquals(r.readName(), "key");});
        doIt(r -> { Assert.assertEquals(r.readString(), "value");});

        bsonMark.reset();
        docMark.reset();

        doIt(r -> { Assert.assertEquals(r.readName(), "key");});
        doIt(r -> { Assert.assertEquals(r.readString(), "value");});

        doIt(r -> { Assert.assertEquals(r.readName(), "nested");});
        doIt(r -> { Assert.assertEquals(r.readString(), "detsen");});

        doIt(r -> { r.readEndDocument();});
    }

    @Test
    public void databaseRead() {
        getDs().getMapper().map(LogHolder.class, Log.class);
        LogHolder holder = new LogHolder();
        for (int i = 0; i < 1; i++) {
            holder.getLogs().add(new Log(i));
        }
        getDs().save(holder);
        MongoCollection<Document> collection = getDatabase()
                                                   .getCollection(LogHolder.class.getSimpleName());

        Document first = collection.find().first();
        System.out.println("********************* first = " + first);

        LogHolder decode = getMapper().getCodecRegistry().get(LogHolder.class)
                                      .decode(new DocumentReader(first), DecoderContext.builder().build());

    }

    private void setup(final Document document) {
        reader = new DocumentReader(document);
        bsonDocumentReader = new BsonDocumentReader(
            document.toBsonDocument(Document.class, getMapper().getCodecRegistry()));
    }

    private void doIt(final Consumer<BsonReader> function) {
        function.accept(bsonDocumentReader);
        function.accept(reader);
    }

}