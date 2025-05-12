package dev.morphia.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.AggregationImpl;
import dev.morphia.aggregation.AggregationOptions;
import dev.morphia.mapping.codec.Conversions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaQuery;
import dev.morphia.query.Operations;
import dev.morphia.query.Query;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.test.util.ActionTestOptions;

import org.bson.BsonInvalidOperationException;
import org.bson.Document;
import org.bson.json.JsonParseException;
import org.bson.json.JsonWriterSettings;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import static java.lang.Character.toLowerCase;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.bson.json.JsonWriterSettings.builder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
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

        List<Document> actual = runPipeline(options, resourceName, pipeline.apply(
                getDs().aggregate(new AggregationOptions().collection(EXAMPLE_TEST_COLLECTION))));

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
        testQuery(new ActionTestOptions(), function);
    }

    public void testQuery(ActionTestOptions options,
            Function<Query<Document>, Query<Document>> function) {

        var resourceName = prepareDatabase(options);

        Query<Document> apply = function.apply(getDs().find(Document.class, options.findOptions().collection(EXAMPLE_TEST_COLLECTION))
                .disableValidation());
        List<Document> actual = runQuery(options, resourceName, apply);

        checkExpected(options, resourceName, actual);
    }

    public void testUpdate(Function<Query<Document>, Query<Document>> function, UpdateOperator... operators) {
        testUpdate(new ActionTestOptions(), function, operators);
    }

    public void testUpdate(ActionTestOptions options,
            Function<Query<Document>, Query<Document>> function, UpdateOperator... operators) {

        var resourceName = prepareDatabase(options);

        Query<Document> query = function.apply(getDs().find(EXAMPLE_TEST_COLLECTION, Document.class)
                .disableValidation());
        List<Document> actual = runUpdate(options, resourceName, query, options.findOptions(), operators);

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
        String indexFile = "%s/%s/index.json".formatted(prefix(), resourceName);
        List<Document> documents = loadJson(indexFile, "index", false);
        if (documents.size() == 1) {
            collection.createIndex(documents.get(0));
        } else if (documents.size() == 2) {
            collection.createIndex(documents.get(0), indexOptions(documents.get(1)));
        } else if (documents.size() > 2) {
            throw new UnsupportedOperationException("more than 2 docs in an index file: " + indexFile);
        }
    }

    private IndexOptions indexOptions(Document document) {
        var options = new IndexOptions();
        document.keySet().forEach(key -> {
            var method = stream(IndexOptions.class.getMethods())
                    .filter(m -> m.getName().equals(key) && m.getParameterCount() == 1)
                    .findFirst().orElseThrow();
            try {
                method.invoke(options, Conversions.convert(document.get(key), method.getParameterTypes()[0]));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
        return options;
    }

    protected void loadIndex(String resourceName, String databaseName, String collectionName) {
        MongoCollection<Document> collection = getDatabase(databaseName).getCollection(collectionName);
        List<Document> documents = loadJson("%s/%s/index.json".formatted(prefix(), resourceName), "index", false);
        documents.forEach(document -> assertNotNull(collection.createIndex(document)));
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
            try {
                String expected = toJson(pipeline);
                String actual = toJson(target);

                toFile("actual", pipeline);
                toFile("expected", target);

                JSONAssert.assertEquals("Should generate the same pipeline", actual, expected, false);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        if (!options.skipDataCheck()) {
            return aggregation.toList();
        } else {
            return emptyList();
        }
    }

    @SuppressWarnings({ "rawtypes" })
    protected List<Document> runQuery(ActionTestOptions testOptions, String pipelineTemplate, Query<Document> query) {
        String resourceName = format("%s/%s/action.json", prefix(), pipelineTemplate);
        Document document = ((MorphiaQuery) query).toDocument();

        if (!testOptions.skipActionCheck()) {
            Document target = loadQuery(resourceName);
            assertEquals(toJson(document), toJson(target), "Should generate the same query document");
        }

        if (!testOptions.skipDataCheck()) {
            try (var cursor = query.iterator()) {
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

        checkAction(testOptions, resourceName, document, operators);

        if (!testOptions.skipDataCheck()) {
            var resource = loadResource(resourceName).stream().collect(joining());
            query.update(testOptions.updateOptions().multi(resource.contains("updateMany")), operators);
            try (var cursor = query.iterator()) {
                return cursor.toList();
            }
        } else {
            return emptyList();
        }
    }

    private void checkAction(ActionTestOptions testOptions,
            String resourceName,
            Document document,
            UpdateOperator... operators) {
        if (!testOptions.skipActionCheck()) {
            List<Document> action = loadAction(resourceName);
            assertEquals(toJson(document), toJson(action.get(0)), "Should generate the same query document");
            Operations operations = new Operations(getDs(), null, asList(operators), false);
            Document updates = operations.toDocument(getDs());
            assertEquals(toJson(updates), toJson(action.get(1)), "Should generate the same update document");
        }
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
                toFile("actual", actual);
                toFile("expected", expected);

                JSONAssert.assertEquals(toJson(expected), toJson(actual), options.orderMatters());
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
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
        var current = "";
        try (var lines = new StringReader(String.join("", resource))) {
            int read;
            while ((read = lines.read()) != -1) {
                char c = (char) read;
                if (!List.of(',', ' ', '\n').contains(c) || !current.isEmpty()) {

                    current += c;
                    if (balanced(current)) {
                        try {
                            docs.add(Document.parse(current));
                            current = "";
                        } catch (JsonParseException | BsonInvalidOperationException e) {
                            throw new RuntimeException("Error parsing " + current, e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        if (!options.skipIndex()) {
            loadIndex(resourceName, EXAMPLE_TEST_COLLECTION);
        }
        return resourceName;
    }
}
