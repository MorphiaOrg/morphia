package dev.morphia.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The audits tests run the same vectors through {@code PublishedComparison}, so the two ports agree. */
public class PrintedResultsTest {
    @Test
    public void goldenVectors() throws IOException {
        BsonDocument vectors;
        try (InputStream stream = getClass().getResourceAsStream("/dev/morphia/test/generated/vectors/printed.json")) {
            vectors = BsonDocument.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        for (BsonValue value : vectors.getArray("vectors")) {
            BsonDocument vector = value.asDocument();
            String verdict;
            try {
                verdict = PrintedResults.difference(vector.getArray("constraints").getValues(),
                        vector.getArray("actual").getValues(),
                        vector.getString("order").getValue().equals("ORDERED")) == null ? "EQUAL" : "DIFFERENT";
            } catch (IllegalArgumentException | ClassCastException | org.bson.BsonInvalidOperationException e) {
                verdict = "INVALID";
            }
            assertEquals(vector.getString("verdict").getValue(), verdict, vector.getString("name").getValue());
        }
    }
}
