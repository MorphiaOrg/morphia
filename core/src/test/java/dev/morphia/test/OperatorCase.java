package dev.morphia.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.bson.BsonDocument;

/**
 * Describes one documented operator example: the absolute classpath resource of its {@code scenario.json}, either in
 * the generated namespace or, for a converted legacy case, in its legacy fixture directory. The scenario carries everything else the runner
 * needs (data, action,
 * expected results, comparison policy and requirements), so nothing is looked up by test method name.
 *
 * @param resource the absolute classpath resource path of the scenario
 */
public record OperatorCase(String resource) {
    private static final java.util.regex.Pattern LEGACY = java.util.regex.Pattern.compile(
            "/dev/morphia/test/(aggregation/expressions|aggregation/stages|query/filters|query/updates)/[^/]+/[^/]+/scenario\\.json");

    public OperatorCase {
        boolean generated = resource.startsWith("/dev/morphia/test/generated/fixtures/") && resource.endsWith("/scenario.json");
        if (!generated && !LEGACY.matcher(resource).matches()) {
            throw new IllegalArgumentException("Not a scenario resource: " + resource);
        }
    }

    public static OperatorCase of(String resource) {
        return new OperatorCase(resource);
    }

    /**
     * @return the parsed scenario
     */
    public BsonDocument scenario() {
        try (InputStream stream = OperatorCase.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing scenario " + resource);
            }
            BsonDocument scenario = BsonDocument.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            if (scenario.getInt32("schemaVersion").getValue() != 1) {
                throw new IllegalStateException("Unsupported scenario schema in " + resource);
            }
            return scenario;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + resource, e);
        }
    }
}
