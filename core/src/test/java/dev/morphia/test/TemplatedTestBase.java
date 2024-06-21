package dev.morphia.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.lang.Nullable;

import dev.morphia.UpdateOptions;
import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.AggregationImpl;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaQuery;
import dev.morphia.query.Query;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.test.util.ActionTestOptions;
import dev.morphia.test.util.Comparanator;

import org.bson.BsonInvalidOperationException;
import org.bson.Document;
import org.bson.json.JsonParseException;
import org.bson.json.JsonWriterSettings;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import static java.lang.Character.toLowerCase;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.bson.json.JsonWriterSettings.builder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public abstract class TemplatedTestBase extends TestBase {
    public static final JsonWriterSettings JSON_WRITER_SETTINGS = builder()
            .indent(true)
            .build();

    protected static final String EXAMPLE_TEST_COLLECTION = "example_test";

    protected final ObjectMapper mapper = new ObjectMapper();

    public TemplatedTestBase() {
        buildConfig()
                .codecProvider(new ZDTCodecProvider());
    }

    protected static String toString(List<Document> actual, String prefix) {
        return actual.stream()
                .map(c -> c.toJson(JSON_WRITER_SETTINGS))
                .collect(joining("\n\t", prefix, ""));
    }

    private static List<String> unwrapArray(List<String> resource) {
        String line = resource.get(0).trim();
        if (line.startsWith("[")) {
            resource.set(0, line.trim().substring(1));
        }
        var last = resource.size() - 1;
        line = resource.get(last).trim();
        if (line.endsWith("]")) {
            resource.set(last, line.substring(0, line.length() - 1));
        }

        return resource;
    }

    @AfterClass
    public void testCoverage() {
        var type = getClass();
        var methods = stream(type.getDeclaredMethods())
                .filter(m -> m.getName().startsWith("testExample"))
                .map(m -> {
                    String name = m.getName().substring(4);
                    return toLowerCase(name.charAt(0)) + name.substring(1);
                })
                .toList();
        String path = type.getPackageName();
        String simpleName = type.getSimpleName().substring(4);
        var operatorName = toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        var resourceFolder = TemplatedTestBase.rootToCore("src/test/resources/%s/%s".formatted(path.replace('.', '/'), operatorName));

        if (!resourceFolder.exists()) {
            throw new IllegalStateException("%s does not exist inside %s".formatted(resourceFolder,
                    new File(".").getAbsolutePath()));
        }
        List<File> list = Arrays.stream(resourceFolder.list())
                .map(s -> new File(resourceFolder, s))
                .toList();

        List<String> examples = list.stream()
                .filter(d -> new File(d, "action.json").exists())
                .map(File::getName)
                .toList();
        var missing = examples.stream()
                .filter(example -> !methods.contains(example))
                .collect(joining(", "));
        if (!missing.isEmpty()) {
            fail("Missing test cases for $%s: %s".formatted(operatorName, missing));
        }
    }

    @NotNull
    public static File rootToCore(String path) {
        return new File(CORE_ROOT, path);
    }

    public void testPipeline(Function<Aggregation<Document>, Aggregation<Document>> pipeline) {
        testPipeline(new ActionTestOptions(), pipeline);
    }

    public void testPipeline(ActionTestOptions options,
            Function<Aggregation<Document>, Aggregation<Document>> pipeline) {
        var resourceName = prepareDatabase(options);

        validateTestName(resourceName);

        List<Document> actual = runPipeline(options, resourceName, pipeline.apply(getDs().aggregate(EXAMPLE_TEST_COLLECTION)));

        checkExpected(options, resourceName, actual);
    }

    protected String discoverResourceName() {
        var method = findTestMethod();
        String methodName = method.getName();

        if (methodName.startsWith("test")) {
            methodName = methodName.substring(4);
            methodName = methodName.substring(0, 1).toLowerCase() + methodName.substring(1);
        }
        return methodName;
    }

    protected void loadData(String resourceName, String collection) {
        insert(collection, loadJson(format("%s/%s/data.json", prefix(), resourceName), "data", true));
    }

    protected void loadData(String collection, int index) {
        insert(collection, loadJson(format("%s/%s/data%d.json", prefix(), discoverResourceName(), index), "data", true));
    }

    protected @NotNull List<Document> loadExpected(String resourceName) {
        return loadJson("%s/%s/expected.json".formatted(prefix(), resourceName), "expected", true);
    }

    protected Method findTestMethod() {
        return stream(new Exception().getStackTrace())
                .map(this::isTestMethod)
                .filter(Objects::nonNull)
                .findFirst()
                .get();
    }

    @NotNull
    protected List<Document> loadJson(String name, String type, boolean failOnMissing) {
        List<Document> data = new ArrayList<>();
        InputStream stream = getClass().getResourceAsStream(name);
        if (stream == null) {
            if (failOnMissing) {
                fail(format("missing " + type + " file: src/test/resources/%s/%s",
                        getClass().getPackageName().replace('.', '/'), name));
            }
        } else {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                while (reader.ready()) {
                    String json = reader.readLine();
                    try {
                        if (json.startsWith("[") && json.endsWith("]")) {
                            json = json.substring(1, json.length() - 1);
                        }
                        data.add(Document.parse(json));
                    } catch (JsonParseException e) {
                        throw new JsonParseException(e.getMessage() + "\n" + json, e);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return data;
    }

    public final String prefix() {
        String root = getClass().getSimpleName().replace("Test", "");
        return toLowerCase(root.charAt(0)) + root.substring(1);
    }

    @Nullable
    private Method isTestMethod(StackTraceElement element) {
        try {
            Class<?> klass = Class.forName(element.getClassName());
            Method method = klass.getDeclaredMethod(element.getMethodName());

            return method.getAnnotation(Test.class) != null ? method : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public void testQuery(Function<Query<Document>, Query<Document>> function) {
        testQuery(function);
    }

    public void testQuery(ActionTestOptions options,
            Function<Query<Document>, Query<Document>> function) {

        var resourceName = prepareDatabase(options);

        Query<Document> apply = function.apply(getDs().find(EXAMPLE_TEST_COLLECTION, Document.class)
                .disableValidation());
        List<Document> actual = runQuery(options, resourceName, apply, options.findOptions());

        checkExpected(options, resourceName, actual);
    }

    public void testUpdate(Function<Query<Document>, Query<Document>> function, UpdateOperator... operators) {
        testUpdate(function, operators);
    }

    public void testUpdate(ActionTestOptions options,
            Function<Query<Document>, Query<Document>> function, UpdateOperator... operators) {

        var resourceName = prepareDatabase(options);

        Query<Document> apply = function.apply(getDs().find(EXAMPLE_TEST_COLLECTION, Document.class)
                .disableValidation());
        List<Document> actual = runUpdate(options, resourceName, apply, options.findOptions(), operators);

        checkExpected(options, resourceName, actual);
    }

    protected List<Document> loadAction(String actionName) {
        return extractDocuments(unwrapArray(loadResource(actionName)));
    }

    protected void loadData(ActionTestOptions options, String collection, int index) {
        if (!options.skipDataCheck()) {
            insert(collection, loadJson(format("%s/%s/data%d.json", prefix(), discoverResourceName(), index), "data", true));
        }
    }

    protected void loadIndex(String resourceName, String collectionName) {
        MongoCollection<Document> collection = getDatabase().getCollection(collectionName);
        List<Document> documents = loadJson("%s/%s/index.json".formatted(prefix(), resourceName), "index", false);
        documents.forEach(document -> collection.createIndex(document));
    }

    protected Document loadQuery(String pipelineName) {
        return loadAction(pipelineName).get(0);
    }

    protected List<String> loadResource(String pipelineName) {
        InputStream stream = getClass().getResourceAsStream(pipelineName);
        if (stream == null) {
            fail(format("missing action file: src/test/resources/%s/%s", getClass().getPackageName().replace('.', '/'),
                    pipelineName));
        }
        return new BufferedReader(new InputStreamReader(stream))
                .lines()
                .collect(toList());
    }

    protected String loadTestName(String resourceName) {
        String name = format("%s/%s/name", prefix(), resourceName);

        InputStream stream = getClass().getResourceAsStream(name);
        if (stream == null) {
            fail(format("missing name file: %s", name));
        }

        try (var reader = new BufferedReader(new InputStreamReader(stream))) {
            return reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<Document> runPipeline(ActionTestOptions options, String pipelineTemplate, Aggregation<Document> aggregation) {
        String pipelineName = format("%s/%s/action.json", prefix(), pipelineTemplate);
        List<Document> pipeline = ((AggregationImpl) aggregation).pipeline();

        if (!options.skipActionCheck()) {
            List<Document> target = loadAction(pipelineName);
            assertEquals(toJson(pipeline), toJson(target), "Should generate the same pipeline");
        }

        if (!options.skipDataCheck()) {
            try (var cursor = aggregation.execute(Document.class)) {
                return cursor.toList();
            }
        } else {
            return emptyList();
        }
    }

    @SuppressWarnings({ "rawtypes" })
    protected List<Document> runQuery(ActionTestOptions testOptions, String pipelineTemplate, Query<Document> query, FindOptions options) {
        String resourceName = format("%s/%s/action.json", prefix(), pipelineTemplate);
        Document document = ((MorphiaQuery) query).toDocument();

        if (!testOptions.skipActionCheck()) {
            Document target = loadQuery(resourceName);
            assertEquals(toJson(document), toJson(target), "Should generate the same query document");
        }

        if (!testOptions.skipDataCheck()) {
            try (var cursor = query.iterator(options)) {
                return cursor.toList();
            }
        } else {
            return emptyList();
        }
    }

    @SuppressWarnings({ "rawtypes" })
    protected List<Document> runUpdate(ActionTestOptions testOptions, String pipelineTemplate, Query<Document> query,
            FindOptions options, UpdateOperator... operators) {
        String resourceName = format("%s/%s/action.json", prefix(), pipelineTemplate);
        Document document = ((MorphiaQuery) query).toDocument();

        List<Document> action = loadAction(resourceName);
        if (!testOptions.skipActionCheck()) {
            assertEquals(toJson(document), toJson(action.get(0)), "Should generate the same query document");
        }

        var first = operators[0];
        var others = Arrays.copyOfRange(operators, 1, operators.length);
        if (!testOptions.skipDataCheck()) {
            var resource = loadResource(resourceName);
            query.update(new UpdateOptions().multi(resource.contains("updateMany")), first, others);
            try (var cursor = getDs().find(EXAMPLE_TEST_COLLECTION, Document.class).iterator()) {
                return cursor.toList();
            }
        } else {
            return emptyList();
        }
    }

    protected String toJson(Document document) {
        return document.toJson(JSON_WRITER_SETTINGS, getDatabase().getCodecRegistry().get(Document.class));
    }

    protected void validateTestName(String resourceName) {
        Method method = findTestMethod();
        Test test = method.getAnnotation(Test.class);
        assertEquals(
                test.testName(), loadTestName(resourceName),
                "%s#%s does not have a name configured on the test.".formatted(method.getDeclaringClass().getName(),
                        method.getName()));
    }

    private boolean balanced(String input) {
        int open = 0;
        int close = 0;
        for (char c : input.toCharArray()) {
            if (c == '{')
                open++;
            if (c == '}')
                close++;
        }

        return open != 0 && open == close;
    }

    private void checkExpected(ActionTestOptions options, String resourceName, List<Document> actual) {
        if (!options.skipDataCheck()) {
            List<Document> expected = loadExpected(resourceName);

            actual = options.removeIds() ? removeIds(actual) : actual;
            expected = options.removeIds() ? removeIds(expected) : expected;

            try {
                Comparanator.of(null, actual, expected, options.orderMatters()).compare();
            } catch (AssertionError e) {
                throw new AssertionError("%s\n\n actual: %s".formatted(e.getMessage(), toString(actual, "\n\t")),
                        e);
            }
        }
    }

    private List<Document> extractDocuments(List<String> resource) {
        var line = resource.get(0).trim();
        if (line.startsWith("db.")) {
            line = line.substring(line.indexOf("(") + 1).trim();
            resource.set(0, line);
            line = resource.get(resource.size() - 1);
            line = line.substring(0, line.lastIndexOf(")"));
            resource.set(resource.size() - 1, line);
        }
        List<Document> docs = new ArrayList<>();
        Iterator<String> lines = resource.iterator();
        var current = "";
        while (lines.hasNext()) {
            current += lines.next();
            if (balanced(current)) {
                try {
                    docs.add(Document.parse(current));
                    current = "";
                } catch (JsonParseException | BsonInvalidOperationException e) {
                    throw new RuntimeException("Error parsing " + current, e);
                }
            }
        }

        return docs;
    }

    private String prepareDatabase(ActionTestOptions options) {
        checkMinServerVersion(options.serverVersion());
        checkMinDriverVersion(options.minDriver());
        var resourceName = discoverResourceName();
        validateTestName(resourceName);
        if (!options.skipDataCheck()) {
            loadData(resourceName, EXAMPLE_TEST_COLLECTION);
        }
        loadIndex(resourceName, EXAMPLE_TEST_COLLECTION);
        return resourceName;
    }

    private String toJson(List<Document> pipeline) {
        return pipeline.stream()
                .map(d -> d.toJson(JSON_WRITER_SETTINGS, getDatabase().getCodecRegistry().get(Document.class)))
                .collect(joining("\n, ", "[\n", "\n]"));
    }
}
