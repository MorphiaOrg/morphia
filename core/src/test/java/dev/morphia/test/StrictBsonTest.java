package dev.morphia.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the shared golden vectors through {@link StrictBson}; the audits tests run the same file through their
 * comparator, so the two cannot drift apart unnoticed.
 */
public class StrictBsonTest {
    @Test
    public void goldenVectors() throws IOException {
        BsonDocument vectors;
        try (InputStream stream = getClass().getResourceAsStream("/dev/morphia/test/generated/vectors/strict.json")) {
            vectors = BsonDocument.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        for (BsonValue value : vectors.getArray("values")) {
            BsonDocument vector = value.asDocument();
            assertEquals(vector.getBoolean("equal").getValue(), StrictBson.equal(vector.get("left"), vector.get("right")),
                    vector.getString("name").getValue());
        }
        for (BsonValue value : vectors.getArray("results")) {
            BsonDocument vector = value.asDocument();
            assertEquals(vector.getBoolean("equal").getValue(),
                    StrictBson.equal(vector.getArray("left").getValues(), vector.getArray("right").getValues(),
                            vector.getBoolean("ordered").getValue(), unorderedArrays(vector)),
                    vector.getString("name").getValue());
        }
    }

    /** A vector's declared unordered arrays, when it declares any. */
    static Set<String> unorderedArrays(BsonDocument vector) {
        Set<String> paths = new TreeSet<>();
        vector.getArray("unorderedArrays", new BsonArray()).forEach(path -> paths.add(path.asString().getValue()));
        return paths;
    }

    /** Extended JSON cannot spell a NaN payload, so this is not a vector; the audits comparator compares raw bits. */
    @Test
    public void nanPayloadsDiffer() {
        BsonDouble quiet = new BsonDouble(Double.longBitsToDouble(0x7ff8000000000000L));
        BsonDouble payload = new BsonDouble(Double.longBitsToDouble(0x7ff8000000000001L));
        assertTrue(StrictBson.equal(quiet, new BsonDouble(Double.NaN)));
        assertFalse(StrictBson.equal(quiet, payload));
    }
}
