package dev.morphia.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mongodb.client.MongoDatabase;
import com.mongodb.lang.Nullable;

import dev.morphia.UpdateOptions;
import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.AggregationImpl;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaQuery;
import dev.morphia.query.Operations;
import dev.morphia.query.Query;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.test.FormRules.Rule;
import dev.morphia.test.FormRules.Shape;

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
 * Arrays named in {@code comparison.unorderedArrays}, by pointer within each result, are compared as multisets in the
 * behavior and published checks. Fields named in {@code comparison.ignore} are removed from both results before the
 * published comparison only, a
 * {@code requirements.minimumServer} skips the case on an older server, and a check the scenario lists under
 * {@code skipped}, with its reason, is not run. This runner supports aggregations with exact or printed expected results
 * and no tolerances, and a dataset only when the scenario skips its behavior check; a scenario that needs either fails rather
 * than being checked loosely. Documented indexes are created on both databases after they are seeded. A write (an
 * update, or a pipeline that writes with {@code $merge} or {@code $out}) can carry {@code action.read}, the find the
 * page runs afterwards to show its effect: both sides run that find after the write, and its results replace the
 * action's own in the behavior and published checks.
 */
public class OperatorCaseRunner {
    // Canonical extended JSON, so a failure shows BSON types: relaxed JSON prints an Int64 like an Int32.
    private static final JsonWriterSettings JSON = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).indent(true).build();

    /** The stages whose argument can name another database, with {@code db} or a {@code {db, coll}} namespace. */
    private static final Set<String> NAMESPACED = Set.of("$out", "$merge", "$lookup", "$graphLookup", "$unionWith");

    private final TestBase test;

    public OperatorCaseRunner(TestBase test) {
        this.test = test;
    }

    public void aggregate(OperatorCase operatorCase, Function<Aggregation<Document>, Aggregation<Document>> dsl) {
        run(operatorCase, "aggregate", Shape.PIPELINE, (action, collection) -> {
            Aggregation<Document> aggregation = dsl.apply(test.getDs().aggregate(aggregationOptions().collection(collection)));
            List<BsonDocument> pipeline = action.getArray("pipeline").getValues().stream().map(BsonValue::asDocument).toList();
            return new Execution(bson(((AggregationImpl<Document>) aggregation).pipeline()),
                    reference -> new ArrayList<>(reference.getCollection(collection, BsonDocument.class).aggregate(pipeline)
                            .into(new ArrayList<>())),
                    () -> bson(aggregation.toList()).getValues());
        });
    }

    /**
     * Runs a query case: the documented filter runs through the driver, and Morphia's query as {@code dsl} builds it.
     */
    public void find(OperatorCase operatorCase, Function<Query<Document>, Query<Document>> dsl) {
        find(operatorCase, dsl, new FindOptions());
    }

    /**
     * Runs a query case whose page documents a projection: Morphia's is {@code options}', compared with the page's
     * after the filter. Any other option would change the results, which the behavior check compares with the
     * documented query's.
     */
    public void find(OperatorCase operatorCase, Function<Query<Document>, Query<Document>> dsl, FindOptions options) {
        run(operatorCase, "find", Shape.FIND, (action, collection) -> {
            FindOptions found = new FindOptions().copy(options).collection(collection);
            Query<Document> query = dsl.apply(test.getDs().find(Document.class, found).disableValidation());
            BsonDocument filter = action.getDocument("filter");
            BsonDocument projection = action.getDocument("projection", null);
            List<Document> emitted = new ArrayList<>(List.of(((MorphiaQuery<Document>) query).toDocument()));
            found.projection().disableValidation(true);
            Document mapped = found.projection().map(test.getMapper(), Document.class);
            if (mapped != null || projection != null) {
                emitted.add(mapped != null ? mapped : new Document());
            }
            return new Execution(bson(emitted),
                    reference -> new ArrayList<>(reference.getCollection(collection, BsonDocument.class).find(filter)
                            .projection(projection).into(new ArrayList<>())),
                    () -> {
                        try (var cursor = query.iterator()) {
                            return bson(cursor.toList()).getValues();
                        }
                    });
        });
    }

    /**
     * Runs an update case: the results are what the page's own read returns after the update, when the scenario
     * carries one, and otherwise the documents its filter matches afterwards. Every collection is compared as well.
     */
    public void update(OperatorCase operatorCase, Function<Query<Document>, Query<Document>> dsl, UpdateOperator... operators) {
        run(operatorCase, "update", Shape.UPDATE, (action, collection) -> {
            Query<Document> query = dsl.apply(test.getDs().find(collection, Document.class).disableValidation());
            Document updates = new Operations(test.getDs(), null, List.of(operators), false).toDocument(test.getDs());
            BsonDocument filter = action.getDocument("filter");
            BsonDocument update = action.getDocument("update");
            boolean multi = action.getBoolean("multi").getValue();
            // The page's read, when it shows one, gives the results; the filter isn't queried again.
            boolean read = action.containsKey("read");
            return new Execution(bson(List.of(((MorphiaQuery<Document>) query).toDocument(), updates)),
                    reference -> {
                        var target = reference.getCollection(collection, BsonDocument.class);
                        if (multi) {
                            target.updateMany(filter, update);
                        } else {
                            target.updateOne(filter, update);
                        }
                        return read ? List.of() : new ArrayList<>(target.find(filter).into(new ArrayList<>()));
                    },
                    () -> {
                        query.update(new UpdateOptions().multi(multi), operators);
                        if (read) {
                            return List.of();
                        }
                        try (var cursor = query.iterator()) {
                            return bson(cursor.toList()).getValues();
                        }
                    });
        });
    }

    /** The documents the page's read after a write returns from {@code database}. */
    private static List<BsonValue> read(MongoDatabase database, BsonDocument read) {
        var find = database.getCollection(read.getString("collection").getValue(), BsonDocument.class)
                .find(read.getDocument("filter"));
        if (read.containsKey("projection")) {
            find = find.projection(read.getDocument("projection"));
        }
        if (read.containsKey("sort")) {
            find = find.sort(read.getDocument("sort"));
        }
        return new ArrayList<>(find.into(new ArrayList<>()));
    }

    /** What one kind of action contributes to a run: Morphia's command, and each side's results. */
    private record Execution(BsonArray emitted, Function<MongoDatabase, List<BsonValue>> reference,
            Supplier<List<BsonValue>> morphia) {
    }

    /** The documented command of each action kind, in the order its shape reads it. */
    private static BsonArray documented(BsonDocument action) {
        return switch (action.getString("kind").getValue()) {
            case "aggregate" -> action.getArray("pipeline");
            case "find" -> action.containsKey("projection")
                    ? new BsonArray(List.of(action.getDocument("filter"), action.getDocument("projection")))
                    : new BsonArray(List.of(action.getDocument("filter")));
            default -> new BsonArray(List.of(action.getDocument("filter"), action.getDocument("update")));
        };
    }

    private void run(OperatorCase operatorCase, String kind, Shape shape,
            BiFunction<BsonDocument, String, Execution> plan) {
        BsonDocument scenario = operatorCase.scenario();
        supported(operatorCase, scenario);
        BsonDocument action = scenario.getDocument("action");
        if (!action.getString("kind").getValue().equals(kind)) {
            fail(operatorCase.resource() + ": its action is not " + kind);
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
        Set<String> unordered = new TreeSet<>();
        scenario.getDocument("comparison").getArray("unorderedArrays", new BsonArray())
                .forEach(path -> unordered.add(path.asString().getValue()));

        MongoDatabase morphia = test.getDatabase();
        MongoDatabase reference = test.getDatabase(morphia.getName() + "_reference");
        reference.drop();
        try {
            // Ids the documented data leaves to the server are assigned once, so both databases hold the same
            // documents and the parity checks stay strict.
            BsonDocument seeded = withIds(scenario.getDocument("collections"));
            seed(morphia, seeded);
            seed(reference, seeded);
            index(morphia, scenario.getArray("indexes"));
            index(reference, scenario.getArray("indexes"));

            Execution execution = plan.apply(action, collection);
            // Morphia's own command runs even when its serialization isn't compared, so it is held to the same rule.
            if (kind.equals("aggregate") && execution.emitted().getValues().stream().anyMatch(OperatorCaseRunner::namesADatabase)) {
                fail(operatorCase.resource() + ": Morphia's pipeline names another database: " + json(execution.emitted()));
            }
            BsonArray documented = documented(action);
            // Both sides take the declared rules; the documented command itself is what runs.
            Set<Rule> rules = EnumSet.noneOf(Rule.class);
            scenario.getDocument("comparison").getArray("rules")
                    .forEach(rule -> rules.add(Rule.valueOf(rule.asString().getValue())));
            // A page may document equivalent spellings of the action; Morphia's must equal one of them, and the reference
            // then runs that one.
            List<BsonDocument> spellings = new ArrayList<>(List.of(action));
            action.getArray("alternatives", new BsonArray()).forEach(alternative -> {
                BsonDocument spelling = action.clone();
                spelling.remove("alternatives");
                spelling.putAll(alternative.asDocument());
                spellings.add(spelling);
            });
            BsonArray emitted = execution.emitted();
            BsonDocument matched = spellings.stream()
                    .filter(spelling -> StrictBson.equal(new BsonArray(FormRules.canonical(documented(spelling).getValues(), shape, rules)),
                            new BsonArray(FormRules.canonical(emitted.getValues(), shape, rules))))
                    .findFirst().orElse(null);
            if (!skipped.containsKey("serialization") && matched == null) {
                fail(operatorCase.resource() + ": serialization differs\ndocumented: " + json(documented) + "\nmorphia:    "
                        + json(execution.emitted()));
            }
            if (matched != null && matched != action) {
                execution = plan.apply(matched, collection);
            }
            // The action is not run (a sample dataset, or results the server can't reproduce); the scenario names why.
            if (skipped.containsKey("behavior")) {
                return;
            }

            List<BsonValue> referenceResults = execution.reference().apply(reference);
            List<BsonValue> morphiaResults = execution.morphia().get();
            // The page shows a write's effect with a find of its own; both sides run it after the write, and its
            // results are the ones compared.
            BsonDocument read = action.getDocument("read", null);
            if (read != null) {
                referenceResults = read(reference, read);
                morphiaResults = read(morphia, read);
            }
            if (!StrictBson.equal(referenceResults, morphiaResults, ordered, unordered)) {
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
            // A reviewed page error: the page prints results the server does not return.
            if (skipped.containsKey("published")) {
                return;
            }
            if (expected.containsKey("documents")) {
                List<BsonValue> published = without(ignore, expected.getArray("documents").getValues());
                if (!StrictBson.equal(published, morphiaResults, ordered, unordered)) {
                    fail(operatorCase.resource() + ": results differ from the documented results\ndocumented: "
                            + json(published) + "\nmorphia:    " + json(morphiaResults));
                }
                if (!StrictBson.equal(published, referenceResults, ordered, unordered)) {
                    fail(operatorCase.resource() + ": the reference results differ from the documented results\ndocumented: "
                            + json(published) + "\nreference:  " + json(referenceResults));
                }
            } else if (expected.containsKey("printed")) {
                List<BsonValue> printed = new ArrayList<>();
                for (BsonValue constraint : expected.getArray("printed")) {
                    printed.add(PrintedResults.without(ignore, constraint));
                }
                String difference = PrintedResults.difference(printed, morphiaResults, ordered, unordered);
                if (difference != null) {
                    fail(operatorCase.resource() + ": results differ from the printed results at " + difference
                            + "\nmorphia: " + json(morphiaResults));
                }
                difference = PrintedResults.difference(printed, referenceResults, ordered, unordered);
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
        // A sample dataset is not loaded, so a case that names one must skip its behavior check.
        if (!scenario.get("dataset").isNull() && !scenario.getDocument("skipped", new BsonDocument()).containsKey("behavior")) {
            unsupported.add("dataset");
        }
        for (BsonValue index : scenario.getArray("indexes")) {
            if (!index.isDocument() || !index.asDocument().isString("collection") || !index.asDocument().isDocument("keys")
                    || !Set.of("collection", "keys", "options").containsAll(index.asDocument().keySet())) {
                unsupported.add("index " + index);
            }
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
        for (BsonValue path : comparison.getArray("unorderedArrays", new BsonArray())) {
            if (!path.isString() || !path.asString().getValue().startsWith("/")) {
                unsupported.add("unordered array " + path);
            }
        }
        if (!Set.of("order", "rules", "ignore", "unorderedArrays", "tolerance").containsAll(comparison.keySet())) {
            unsupported.add("comparison fields " + comparison.keySet());
        }
        Set<String> keys = Set.of("schemaVersion", "name", "title", "context", "source", "dataset", "collections", "indexes",
                "action", "expected", "comparison", "requirements", "skipped");
        if (!keys.containsAll(scenario.keySet())) {
            unsupported.add("scenario fields " + scenario.keySet());
        }
        BsonDocument skipped = scenario.getDocument("skipped", new BsonDocument());
        // Without the behavior run there are no results to compare with the published ones.
        if (skipped.containsKey("behavior") && !skipped.containsKey("published")) {
            unsupported.add("skipped behavior without skipping published");
        }
        if (!Set.of("serialization", "behavior", "published").containsAll(skipped.keySet())
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
        Set<String> fields = switch (action.getString("kind", new BsonString("")).getValue()) {
            case "aggregate" -> Set.of("kind", "collection", "pipeline");
            case "find" -> action.containsKey("projection") ? Set.of("kind", "collection", "filter", "projection")
                    : Set.of("kind", "collection", "filter");
            case "update" -> Set.of("kind", "collection", "filter", "update", "multi");
            default -> Set.of();
        };
        // An alternative respells the command only; the collection and the read stay the action's.
        Set<String> spelled = switch (action.getString("kind", new BsonString("")).getValue()) {
            case "aggregate" -> Set.of("pipeline");
            case "find" -> Set.of("filter", "projection");
            case "update" -> Set.of("filter", "update");
            default -> Set.of();
        };
        for (BsonValue alternative : action.getArray("alternatives", new BsonArray())) {
            if (!alternative.isDocument() || alternative.asDocument().isEmpty()
                    || !spelled.containsAll(alternative.asDocument().keySet())) {
                unsupported.add("alternative " + alternative);
            }
        }
        Set<String> present = new TreeSet<>(action.keySet());
        present.remove("alternatives");
        // Only a write has a read after it.
        if (present.remove("read") && (action.getString("kind").getValue().equals("find")
                || !action.get("read").isDocument()
                || !Set.of("collection", "filter", "projection", "sort").containsAll(action.getDocument("read").keySet())
                || !action.getDocument("read").containsKey("collection")
                || !action.getDocument("read").containsKey("filter"))) {
            unsupported.add("read " + action.get("read"));
        }
        if (!present.equals(fields)) {
            unsupported.add("action fields " + action.keySet());
        } else if (action.getString("kind").getValue().equals("aggregate")) {
            List<BsonValue> pipelines = new ArrayList<>(List.of(action.getArray("pipeline")));
            // An alternative spelling runs in place of the action when it matches, so it is held to the same rule.
            for (BsonValue alternative : action.getArray("alternatives", new BsonArray())) {
                if (alternative.isDocument() && alternative.asDocument().containsKey("pipeline")) {
                    pipelines.add(alternative.asDocument().get("pipeline"));
                }
            }
            for (BsonValue pipeline : pipelines) {
                for (BsonValue stage : pipeline.isArray() ? pipeline.asArray().getValues() : List.of(pipeline)) {
                    if (namesADatabase(stage)) {
                        unsupported.add("a stage naming another database: " + stage.asDocument().getFirstKey());
                    }
                }
            }
        }
        schema(operatorCase, scenario, unsupported);
        if (!unsupported.isEmpty()) {
            fail(operatorCase.resource() + ": not supported by this runner: " + unsupported);
        }
    }

    /** The checks of the scenario schema that the run itself doesn't make. */
    private static void schema(OperatorCase operatorCase, BsonDocument scenario, List<String> unsupported) {
        String name = scenario.isString("name") ? scenario.getString("name").getValue() : "";
        if (!operatorCase.resource().equals("/dev/morphia/test/" + name + "/scenario.json")) {
            unsupported.add("name " + scenario.get("name") + " is not its folder");
        }
        if (!scenario.isString("context")
                || !Set.of("EXPRESSION", "STAGE", "FILTER", "UPDATE").contains(scenario.getString("context").getValue())) {
            unsupported.add("context " + scenario.get("context"));
        }
        // The runner's own self-tests are test inputs, not docs examples, so they carry no title or source.
        boolean selfTest = operatorCase.resource().startsWith("/dev/morphia/test/runner/");
        if (!selfTest) {
            if (!scenario.isString("title") || scenario.getString("title").getValue().isBlank()) {
                unsupported.add("title " + scenario.get("title"));
            }
            BsonValue source = scenario.get("source");
            if (!(source instanceof BsonDocument document)
                    || !document.keySet().equals(Set.of("repository", "commit", "path", "blob"))
                    || document.values().stream().anyMatch(value -> !value.isString())
                    || !document.getString("commit").getValue().matches("[0-9a-f]{40}")) {
                unsupported.add("source " + source);
            }
        }
        if (!scenario.getDocument("requirements").keySet().equals(Set.of("minimumServer", "topology"))) {
            unsupported.add("requirements fields " + scenario.getDocument("requirements").keySet());
        }
        BsonDocument expected = scenario.getDocument("expected");
        if (expected.containsKey("none") && !expected.isString("none")) {
            unsupported.add("expected none " + expected.get("none"));
        }
    }

    /**
     * Whether a stage reads or writes outside the case's database ({@code $out}, {@code $merge}, {@code $lookup},
     * {@code $graphLookup} or {@code $unionWith} with a {@code db}), which would make the two behavior runs share
     * state.
     */
    static boolean namesADatabase(BsonValue value) {
        if (value instanceof BsonDocument document) {
            for (var entry : document.entrySet()) {
                // A literal is data, not a stage.
                if (entry.getKey().equals("$literal")) {
                    continue;
                }
                if (NAMESPACED.contains(entry.getKey()) && entry.getValue() instanceof BsonDocument argument
                        && (argument.containsKey("db") || hasDb(argument.get("into")) || hasDb(argument.get("from")))) {
                    return true;
                }
                if (namesADatabase(entry.getValue())) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof BsonArray array) {
            return array.getValues().stream().anyMatch(OperatorCaseRunner::namesADatabase);
        }
        return false;
    }

    private static boolean hasDb(@Nullable BsonValue namespace) {
        return namespace instanceof BsonDocument document && document.containsKey("db");
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

    /**
     * Creates the documented indexes through {@code createIndexes}, so every option the page passes reaches the server
     * as written. An index without a name gets the shell's default, its keys and values joined by underscores.
     */
    private static void index(MongoDatabase database, BsonArray indexes) {
        for (BsonValue value : indexes) {
            BsonDocument index = value.asDocument();
            BsonDocument keys = index.getDocument("keys");
            BsonDocument spec = new BsonDocument("key", keys);
            BsonDocument options = index.getDocument("options", new BsonDocument());
            if (!options.containsKey("name")) {
                List<String> parts = new ArrayList<>();
                keys.forEach((field, order) -> {
                    parts.add(field);
                    parts.add(order.isString() ? order.asString().getValue() : String.valueOf(order.asNumber().intValue()));
                });
                spec.put("name", new BsonString(String.join("_", parts)));
            }
            spec.putAll(options);
            database.runCommand(new BsonDocument("createIndexes", index.getString("collection"))
                    .append("indexes", new BsonArray(List.of(spec))));
        }
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
