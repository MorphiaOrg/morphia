package dev.morphia.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.mongodb.client.MongoDatabase;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.AggregationImpl;
import dev.morphia.test.FormRules.Rule;
import dev.morphia.test.FormRules.Shape;
import dev.morphia.test.util.ActionTestOptions;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonObjectId;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;

import static dev.morphia.aggregation.AggregationOptions.aggregationOptions;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Runs an {@link OperatorCase} and checks three things separately:
 * <ol>
 * <li><b>serialization</b>: the BSON Morphia emits equals the scenario's documented action, strictly (BSON types and
 * key order);</li>
 * <li><b>behavior</b>: the documented action, sent through the driver, and the Morphia operation run against two
 * identically seeded databases and leave the same results and collections;</li>
 * <li><b>published expectation</b>: both results equal the documented results, or satisfy what the printed results
 * prove ({@link PrintedResults}).</li>
 * </ol>
 * Serialization is compared after the scenario's declared form rules ({@link FormRules}) are applied to both sides.
 * Fields named in {@code comparison.ignore} are removed from both results before the published comparison only, a
 * {@code requirements.minimumServer} skips the case on an older server, and a check the scenario lists under
 * {@code skipped}, with its reason, is not run. This runner supports aggregations with exact or printed expected results
 * and no tolerances, datasets or indexes; a scenario that needs any of them fails rather than being checked loosely.
 */
public class OperatorCaseRunner {
    // Canonical extended JSON, so a failure shows BSON types: relaxed JSON prints an Int64 like an Int32.
    private static final JsonWriterSettings JSON = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).indent(true).build();

    private final TestBase test;

    public OperatorCaseRunner(TestBase test) {
        this.test = test;
    }

    /**
     * Runs a converted legacy case: its unchanged test body's options must say what its scenario says, because the
     * scenario, not the options, now drives the run.
     */
    public void aggregate(OperatorCase operatorCase, ActionTestOptions options,
            Function<Aggregation<Document>, Aggregation<Document>> dsl) {
        BsonDocument scenario = operatorCase.scenario();
        BsonDocument comparison = scenario.getDocument("comparison");
        BsonDocument skipped = scenario.getDocument("skipped", new BsonDocument());
        BsonValue minimumServer = scenario.getDocument("requirements").get("minimumServer");
        List<String> differences = new ArrayList<>();
        if (options.orderMatters() != comparison.getString("order").getValue().equals("ORDERED")) {
            differences.add("orderMatters");
        }
        if (options.removeIds() != comparison.getArray("ignore").contains(new BsonString("_id"))) {
            differences.add("removeIds");
        }
        if (options.skipActionCheck() != skipped.containsKey("serialization")) {
            differences.add("skipActionCheck");
        }
        if (options.skipDataCheck() != skipped.containsKey("results")) {
            differences.add("skipDataCheck");
        }
        if (!options.serverVersion().equals(minimumServer.isNull() ? "0.0.0" : minimumServer.asString().getValue())) {
            differences.add("serverVersion");
        }
        // skipDataCheck(true) also sets skipIndex; only an explicit skipIndex is unmapped.
        if (options.skipIndex() && !options.skipDataCheck() || !options.minDriver().equals("0.0.0")) {
            differences.add("skipIndex/minDriver");
        }
        if (!differences.isEmpty()) {
            fail(operatorCase.resource() + ": the test's options differ from its scenario: " + differences);
        }
        aggregate(operatorCase, dsl);
    }

    public void aggregate(OperatorCase operatorCase, Function<Aggregation<Document>, Aggregation<Document>> dsl) {
        BsonDocument scenario = operatorCase.scenario();
        supported(operatorCase, scenario);
        BsonDocument action = scenario.getDocument("action");
        if (!action.getString("kind").getValue().equals("aggregate")) {
            fail(operatorCase.resource() + ": not an aggregation");
        }
        String collection = action.getString("collection").getValue();
        boolean ordered = scenario.getDocument("comparison").getString("order").getValue().equals("ORDERED");
        BsonValue minimumServer = scenario.getDocument("requirements").get("minimumServer");
        if (!minimumServer.isNull()) {
            test.checkMinServerVersion(minimumServer.asString().getValue());
        }
        BsonDocument skipped = scenario.getDocument("skipped", new BsonDocument());
        Set<String> ignore = new TreeSet<>();
        scenario.getDocument("comparison").getArray("ignore").forEach(field -> ignore.add(field.asString().getValue()));

        MongoDatabase morphia = test.getDatabase();
        MongoDatabase reference = test.getDatabase(morphia.getName() + "_reference");
        reference.drop();
        try {
            // Ids the documented data leaves to the server are assigned once, so both databases hold the same
            // documents and the parity checks stay strict.
            BsonDocument seeded = withIds(scenario.getDocument("collections"));
            seed(morphia, seeded);
            seed(reference, seeded);

            Aggregation<Document> aggregation = dsl.apply(test.getDs().aggregate(aggregationOptions().collection(collection)));
            BsonArray emitted = bson(((AggregationImpl<Document>) aggregation).pipeline());
            BsonArray documented = action.getArray("pipeline");
            // Both sides take the declared rules; the documented pipeline itself is what runs.
            Set<Rule> rules = EnumSet.noneOf(Rule.class);
            scenario.getDocument("comparison").getArray("rules")
                    .forEach(rule -> rules.add(Rule.valueOf(rule.asString().getValue())));
            if (!skipped.containsKey("serialization")
                    && !StrictBson.equal(new BsonArray(FormRules.canonical(documented.getValues(), Shape.PIPELINE, rules)),
                            new BsonArray(FormRules.canonical(emitted.getValues(), Shape.PIPELINE, rules)))) {
                fail(operatorCase.resource() + ": serialization differs\ndocumented: " + json(documented) + "\nmorphia:    "
                        + json(emitted));
            }
            // The legacy test did not run the action, so neither does its case; the scenario names why.
            if (skipped.containsKey("results")) {
                return;
            }

            List<BsonValue> referenceResults = new ArrayList<>(reference.getCollection(collection, BsonDocument.class)
                    .aggregate(documented.getValues().stream().map(BsonValue::asDocument).toList())
                    .into(new ArrayList<>()));
            List<BsonValue> morphiaResults = bson(aggregation.toList()).getValues();
            if (!StrictBson.equal(referenceResults, morphiaResults, ordered)) {
                fail(operatorCase.resource() + ": behavior differs\nreference: " + json(referenceResults) + "\nmorphia:   "
                        + json(morphiaResults));
            }
            Set<String> collections = new TreeSet<>();
            morphia.listCollectionNames().into(collections);
            reference.listCollectionNames().into(collections);
            for (String name : collections) {
                List<BsonValue> left = contents(reference, name);
                List<BsonValue> right = contents(morphia, name);
                if (!StrictBson.equal(left, right, false)) {
                    fail(operatorCase.resource() + ": collection " + name + " differs after the action\nreference: " + json(left)
                            + "\nmorphia:   " + json(right));
                }
            }

            // Ignored fields (ids the page shows but the run generates) leave only the published comparison.
            referenceResults = without(ignore, referenceResults);
            morphiaResults = without(ignore, morphiaResults);
            BsonDocument expected = scenario.getDocument("expected");
            if (expected.containsKey("documents")) {
                List<BsonValue> published = without(ignore, expected.getArray("documents").getValues());
                if (!StrictBson.equal(published, morphiaResults, ordered)) {
                    fail(operatorCase.resource() + ": results differ from the documented results\ndocumented: "
                            + json(published) + "\nmorphia:    " + json(morphiaResults));
                }
                if (!StrictBson.equal(published, referenceResults, ordered)) {
                    fail(operatorCase.resource() + ": the reference results differ from the documented results\ndocumented: "
                            + json(published) + "\nreference:  " + json(referenceResults));
                }
            } else if (expected.containsKey("printed")) {
                List<BsonValue> printed = new ArrayList<>();
                for (BsonValue constraint : expected.getArray("printed")) {
                    printed.add(PrintedResults.without(ignore, constraint));
                }
                String difference = PrintedResults.difference(printed, morphiaResults, ordered);
                if (difference != null) {
                    fail(operatorCase.resource() + ": results differ from the printed results at " + difference
                            + "\nmorphia: " + json(morphiaResults));
                }
                difference = PrintedResults.difference(printed, referenceResults, ordered);
                if (difference != null) {
                    fail(operatorCase.resource() + ": the reference results differ from the printed results at "
                            + difference + "\nreference: " + json(referenceResults));
                }
            }
        } finally {
            reference.drop();
        }
    }

    private static void supported(OperatorCase operatorCase, BsonDocument scenario) {
        BsonDocument comparison = scenario.getDocument("comparison");
        List<String> unsupported = new ArrayList<>();
        if (!scenario.get("dataset").isNull()) {
            unsupported.add("dataset");
        }
        if (!scenario.getArray("indexes").isEmpty()) {
            unsupported.add("indexes");
        }
        for (BsonValue rule : comparison.getArray("rules")) {
            if (Arrays.stream(Rule.values()).noneMatch(known -> known.name().equals(rule.asString().getValue()))) {
                unsupported.add("rule " + rule.asString().getValue());
            }
        }
        for (BsonValue field : comparison.getArray("ignore")) {
            if (!field.isString() || field.asString().getValue().contains(".")) {
                unsupported.add("ignored field " + field);
            }
        }
        Set<String> keys = Set.of("schemaVersion", "name", "title", "context", "source", "dataset", "collections", "indexes",
                "action", "expected", "comparison", "requirements", "skipped");
        if (!keys.containsAll(scenario.keySet())) {
            unsupported.add("scenario fields " + scenario.keySet());
        }
        BsonDocument skipped = scenario.getDocument("skipped", new BsonDocument());
        if (!Set.of("serialization", "results").containsAll(skipped.keySet())
                || skipped.values().stream().anyMatch(reason -> !reason.isString() || reason.asString().getValue().isBlank())) {
            unsupported.add("skipped " + skipped);
        }
        if (!comparison.get("tolerance").isNull()) {
            unsupported.add("tolerance");
        }
        BsonDocument expected = scenario.getDocument("expected");
        if (expected.size() != 1 || !Set.of("documents", "printed", "none").contains(expected.getFirstKey())) {
            unsupported.add("expected " + expected.keySet());
        }
        BsonDocument requirements = scenario.getDocument("requirements");
        if (!requirements.get("minimumServer").isNull() && !requirements.get("minimumServer").isString()) {
            unsupported.add("minimum server " + requirements.get("minimumServer"));
        }
        if (!requirements.getString("topology").getValue().equals("STANDALONE")) {
            unsupported.add("topology " + requirements.getString("topology").getValue());
        }
        String order = comparison.getString("order").getValue();
        if (!order.equals("ORDERED") && !order.equals("UNORDERED")) {
            unsupported.add("order " + order);
        }
        BsonDocument action = scenario.getDocument("action");
        if (!action.keySet().equals(Set.of("kind", "collection", "pipeline"))) {
            unsupported.add("action fields " + action.keySet());
        } else {
            for (BsonValue stage : action.getArray("pipeline")) {
                if (namesADatabase(stage)) {
                    unsupported.add("a stage naming another database: " + stage.asDocument().getFirstKey());
                }
            }
        }
        if (!unsupported.isEmpty()) {
            fail(operatorCase.resource() + ": not supported by this runner: " + unsupported);
        }
    }

    /**
     * Whether a stage reads or writes outside the case's database ({@code $out}, {@code $merge}, {@code $lookup},
     * {@code $graphLookup} or {@code $unionWith} with a {@code db}), which would make the two behavior runs share
     * state.
     */
    private static boolean namesADatabase(BsonValue value) {
        if (value instanceof BsonDocument document) {
            if (document.containsKey("db")) {
                return true;
            }
            return document.values().stream().anyMatch(OperatorCaseRunner::namesADatabase);
        }
        if (value instanceof BsonArray array) {
            return array.getValues().stream().anyMatch(OperatorCaseRunner::namesADatabase);
        }
        return false;
    }

    private void seed(MongoDatabase database, BsonDocument collections) {
        collections.forEach((name, rows) -> {
            var target = database.getCollection(name, BsonDocument.class);
            target.drop();
            if (!rows.asArray().isEmpty()) {
                target.insertMany(rows.asArray().getValues().stream().map(BsonValue::asDocument).toList());
            }
        });
    }

    /** {@code collections} with an {@code _id} given to each document that has none. */
    private static BsonDocument withIds(BsonDocument collections) {
        BsonDocument result = new BsonDocument();
        collections.forEach((name, rows) -> {
            BsonArray copy = new BsonArray();
            for (BsonValue row : rows.asArray()) {
                BsonDocument document = row.asDocument();
                if (document.containsKey("_id")) {
                    copy.add(document);
                } else {
                    BsonDocument identified = new BsonDocument("_id", new BsonObjectId(new ObjectId()));
                    identified.putAll(document);
                    copy.add(identified);
                }
            }
            result.put(name, copy);
        });
        return result;
    }

    private static List<BsonValue> contents(MongoDatabase database, String name) {
        return new ArrayList<>(database.getCollection(name, BsonDocument.class).find().into(new ArrayList<>()));
    }

    private BsonArray bson(List<Document> documents) {
        CodecRegistry registry = test.getDs().getCodecRegistry();
        return new Document("v", documents).toBsonDocument(BsonDocument.class, registry).getArray("v");
    }

    /** {@code values} with the top-level {@code fields} removed from each document. */
    private static List<BsonValue> without(Set<String> fields, List<? extends BsonValue> values) {
        List<BsonValue> result = new ArrayList<>();
        for (BsonValue value : values) {
            if (value instanceof BsonDocument document && !fields.isEmpty()) {
                BsonDocument copy = document.clone();
                fields.forEach(copy::remove);
                result.add(copy);
            } else {
                result.add(value);
            }
        }
        return result;
    }

    private static String json(List<? extends BsonValue> values) {
        return new BsonDocument("v", new BsonArray(new ArrayList<>(values))).toJson(JSON);
    }
}
