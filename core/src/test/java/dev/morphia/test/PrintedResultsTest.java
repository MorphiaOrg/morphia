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

    /** An ignored field (a generated id) is dropped from a printed document before it is compared. */
    @Test
    public void ignoredFieldsLeaveThePrintedDocument() {
        BsonValue printed = BsonDocument.parse("{document: [{name: '_id', value: {whole: 1}}, {name: 'a', value: {text: 'x'}}]}");
        BsonValue without = PrintedResults.without(java.util.Set.of("_id"), printed);
        assertEquals(BsonDocument.parse("{document: [{name: 'a', value: {text: 'x'}}]}"), without);
        assertEquals(null, PrintedResults.difference(java.util.List.of(without),
                java.util.List.of(BsonDocument.parse("{a: 'x'}")), true));
    }
}
