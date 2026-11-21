package dev.morphia.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;

import dev.morphia.test.FormRules.Rule;
import dev.morphia.test.FormRules.Shape;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The audits tests run the same vectors through {@code FormEquivalence}, so the two ports agree. */
public class FormRulesTest {
    private static final JsonWriterSettings JSON = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build();

    @Test
    public void goldenVectors() throws IOException {
        BsonDocument vectors;
        try (InputStream stream = getClass().getResourceAsStream("/dev/morphia/test/generated/vectors/rules.json")) {
            vectors = BsonDocument.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        for (BsonValue value : vectors.getArray("vectors")) {
            BsonDocument vector = value.asDocument();
            Set<Rule> rules = EnumSet.noneOf(Rule.class);
            vector.getArray("rules").forEach(rule -> rules.add(Rule.valueOf(rule.asString().getValue())));
            var canonical = FormRules.canonical(decode(vector.getArray("input")).asArray().getValues(),
                    Shape.valueOf(vector.getString("shape").getValue()), rules);
            // Expected outputs are stored as extended JSON text: a {$regex, $options} document would be read back
            // as a regular expression.
            String actual = new BsonDocument("v", new BsonArray(canonical)).toJson(JSON);
            assertEquals(vector.getString("canonical").getValue(), actual, vector.getString("name").getValue());
        }
    }

    /**
     * Reads an input key that starts with {@code #} with {@code $} in its place, since the JSON reader would turn a
     * {@code {$regex, $options}} document into a regular expression.
     */
    private static BsonValue decode(BsonValue value) {
        if (value instanceof BsonDocument document) {
            BsonDocument result = new BsonDocument();
            document.forEach((key, field) -> result.put(key.startsWith("#") ? "$" + key.substring(1) : key, decode(field)));
            return result;
        }
        if (value instanceof BsonArray array) {
            BsonArray result = new BsonArray();
            array.forEach(item -> result.add(decode(item)));
            return result;
        }
        return value;
    }
}
