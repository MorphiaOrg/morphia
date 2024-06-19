package dev.morphia.test.aggregation.stages;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.FullDocumentBeforeChange;

import dev.morphia.aggregation.stages.ChangeStream;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.test.aggregation.AggregationTest;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.ChangeStream.changeStream;
import static dev.morphia.test.DriverVersion.v47;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestChangeStream extends AggregationTest {
    @Test
    @SuppressWarnings("unchecked")
    public void testChangeStream() {
        checkMinDriverVersion(v47);
        checkForReplicaSet();

        Iterator<Document> input = loadJson(format("%s/%s/data.json", prefix(), "changeStream"), "data", true).iterator();
        MongoCollection<Document> collection = getDatabase().getCollection(EXAMPLE_TEST_COLLECTION);

        try (MorphiaCursor<Document> cursor = getDs().aggregate(EXAMPLE_TEST_COLLECTION)
                .changeStream(changeStream())
                .execute(Document.class)) {
            while (input.hasNext()) {
                Document inserted = input.next();
                collection.insertOne(inserted);
                Document next = cursor.next();
                assertDocumentEquals(next.get("fullDocument"), inserted);
            }
        }
    }

    @Test
    public void testChangeStreamOptions() {
        checkMinDriverVersion(v47);
        LocalDateTime startAtOperationTime = now();
        ChangeStream changeStream = changeStream().allChangesForCluster(true)
                .fullDocument(FullDocument.REQUIRED)
                .fullDocumentBeforeChange(FullDocumentBeforeChange.REQUIRED)
                .resumeAfter(new Document("resume", "after"))
                .startAfter(new Document("start", "after"))
                .startAtOperationTime(startAtOperationTime);

        Codec<ChangeStream> codec = getDs().getCodecRegistry().get(ChangeStream.class);

        DocumentWriter writer = new DocumentWriter(getMapper().getConfig());
        codec.encode(writer, changeStream, EncoderContext.builder().build());
        Document document = writer.getDocument().get("$changeStream", Document.class);
        assertTrue(document.getBoolean("allChangesForCluster"));
        assertEquals(document.getString("fullDocument"), FullDocument.REQUIRED.getValue());
        assertEquals(document.getString("fullDocumentBeforeChange"), FullDocumentBeforeChange.REQUIRED.getValue());
        assertEquals(document.get("resumeAfter", Document.class), new Document("resume", "after"));
        assertEquals(document.get("startAfter", Document.class), new Document("start", "after"));
        assertEquals(document.get("startAtOperationTime", LocalDateTime.class), startAtOperationTime.truncatedTo(ChronoUnit.MILLIS));
    }
}
