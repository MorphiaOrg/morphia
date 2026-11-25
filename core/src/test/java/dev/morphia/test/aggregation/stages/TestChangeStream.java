package dev.morphia.test.aggregation.stages;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.FullDocumentBeforeChange;

import dev.morphia.aggregation.stages.ChangeStream;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.test.TemplatedTestBase;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.stages.ChangeStream.changeStream;
import static java.time.LocalDateTime.now;

public class TestChangeStream extends TemplatedTestBase {
    @Test
    public void testChangeStreamOptions() {
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
        Assertions.assertTrue(document.getBoolean("allChangesForCluster"));
        Assertions.assertEquals(FullDocument.REQUIRED.getValue(), document.getString("fullDocument"));
        Assertions.assertEquals(FullDocumentBeforeChange.REQUIRED.getValue(), document.getString("fullDocumentBeforeChange"));
        assertDocumentEquals(new Document("resume", "after"), document.get("resumeAfter", Document.class));
        assertDocumentEquals(new Document("start", "after"), document.get("startAfter", Document.class));
        Assertions.assertEquals(startAtOperationTime.truncatedTo(ChronoUnit.MILLIS),
                document.get("startAtOperationTime", LocalDateTime.class));
    }
}
