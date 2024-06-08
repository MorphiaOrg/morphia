package dev.morphia.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.lang.NonNull;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.AggregationImpl;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaQuery;
import dev.morphia.test.util.Comparanator;

import org.bson.BsonInvalidOperationException;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.json.JsonParseException;
import org.bson.json.JsonWriterSettings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.lang.Character.toLowerCase;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.bson.json.JsonWriterSettings.builder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public abstract class TemplatedTestBase extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(TemplatedTestBase.class);

    public static final JsonWriterSettings JSON_WRITER_SETTINGS = builder()
            .indent(true)
            .build();

    protected static final String AGG_TEST_COLLECTION = "aggtest";

    protected final ObjectMapper mapper = new ObjectMapper();
    private boolean skipPipelineCheck = false;
    private boolean skipDataCheck = false;

    public TemplatedTestBase() {
    }

    public TemplatedTestBase(MorphiaConfig config) {
        super(config);
    }

    @BeforeMethod
    public void resetSkip() {
        skipPipelineCheck = false;
    }

    public final String prefix() {
        String root = getClass().getSimpleName().replace("Test", "");
        return toLowerCase(root.charAt(0)) + root.substring(1);
    }

    public void skipDataCheck() {
        skipDataCheck = true;
    }

    public void skipPipelineCheck() {
        skipPipelineCheck = true;
    }

    public void testPipeline(ServerVersion serverVersion,
            boolean removeIds,
            boolean orderMatters,
            Function<Aggregation<Document>, Aggregation<Document>> pipeline) {
        checkMinServerVersion(serverVersion);
        checkMinDriverVersion(minDriver);
        var resourceName = discoverResourceName(new Exception().getStackTrace());
        loadData(AGG_TEST_COLLECTION);
        loadIndex(AGG_TEST_COLLECTION);

        List<Document> actual = runPipeline(resourceName, pipeline.apply(getDs().aggregate(AGG_TEST_COLLECTION)));

        if (!skipDataCheck) {
            List<Document> expected = loadExpected(resourceName);

            actual = removeIds ? removeIds(actual) : actual;
            expected = removeIds ? removeIds(expected) : expected;

            try {
                Comparanator.of(null, actual, expected, orderMatters).compare();
            } catch (AssertionError e) {
                throw new AssertionError("%s\n\n actual: %s".formatted(e.getMessage(), toString(actual, "\n\t")),
                        e);
            }
        }
    }

    private static String toString(List<Document> actual, String prefix) {
        return actual.stream()
                .map(c -> c.toJson(JSON_WRITER_SETTINGS))
                .collect(joining("\n\t", prefix, ""));
    }

    public <D> void testQuery(MorphiaQuery<D> query, FindOptions options, boolean orderMatters) {
        var resourceName = discoverResourceName(new Exception().getStackTrace());

        loadData(getDs().getCollection(query.getEntityClass()).getNamespace().getCollectionName());

        List<D> actual = runQuery(resourceName, query, options);

        List<D> expected = map(query.getEntityClass(), loadExpected(resourceName));

        if (orderMatters) {
            assertEquals(actual, expected);
        } else {
            assertListEquals(actual, expected);
        }
    }

    protected void loadData(String collection) {
        if (!skipDataCheck) {
            var resourceName = discoverResourceName(new Exception().getStackTrace());
            insert(collection, loadJson(format("%s/%s/data.json", prefix(), resourceName), "data", true));
        }
    }

    protected void loadData(String collection, int index) {
        if (!skipDataCheck) {
            var resourceName = discoverResourceName(new Exception().getStackTrace());
            insert(collection, loadJson(format("%s/%s/data%d.json", prefix(), resourceName, index), "data", true));
        }
    }

    protected void loadIndex(String collectionName) {
        var resourceName = discoverResourceName(new Exception().getStackTrace());
        MongoCollection<Document> collection = getDatabase().getCollection(collectionName);
        List<Document> documents = loadJson("%s/%s/index.json".formatted(prefix(), resourceName), "index", false);
        documents.forEach(document -> {
            collection.createIndex(document);
        });
    }

    protected @NotNull List<Document> loadExpected(String resourceName) {
        return loadJson("%s/%s/expected.json".formatted(prefix(), resourceName), "expected", true);
    }

    protected @NotNull <T> List<T> loadExpected(Class<T> type, String resourceName) {
        return loadJson(type, format("%s/%s/expected.json", prefix(), resourceName));
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

    @NotNull
    protected <T> List<T> loadJson(Class<T> type, String name) {
        List<T> data = new ArrayList<>();
        InputStream stream = getClass().getResourceAsStream(name);
        if (stream == null) {
            fail("missing data file: " + name);
        }
        ObjectMapper mapper = new ObjectMapper();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            while (reader.ready()) {
                data.add(mapper.readValue(reader.readLine(), type));
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return data;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<Document> runPipeline(String pipelineTemplate, Aggregation<Document> aggregation) {
        String pipelineName = format("%s/%s/action.json", prefix(), pipelineTemplate);
        List<Document> pipeline = ((AggregationImpl) aggregation).pipeline();

        if (!skipPipelineCheck) {
            List<Document> target = loadPipeline(pipelineName);
            assertEquals(toJson(pipeline), toJson(target), "Should generate the same pipeline");
        }

        if (!skipDataCheck) {
            try (var cursor = aggregation.execute(Document.class)) {
                return cursor.toList();
            }
        } else {
            return emptyList();
        }
    }

    private String toJson(List<Document> pipeline) {
        return pipeline.stream()
                .map(d -> d.toJson(JSON_WRITER_SETTINGS, getDatabase().getCodecRegistry().get(Document.class)))
                .collect(joining("\n, ", "[\n", "\n]"));
    }

    private List<Document> loadPipeline(String pipelineName) {
        InputStream stream = getClass().getResourceAsStream(pipelineName);
        if (stream == null) {
            fail(format("missing data file: src/test/resources/%s/%s", getClass().getPackageName().replace('.', '/'),
                    pipelineName));
        }

        return parsePipeline(stream);
    }

    private List<Document> parsePipeline(InputStream stream) {
        List<String> list = new ArrayList<>(new BufferedReader(new InputStreamReader(stream))
                .lines()
                .map(String::trim)
                //                .map(line -> line.replaceAll("(\\$*\\w+?):", "\"$1\":"))
                .toList());
        String line = list.get(0);
        if (line.startsWith("[")) {
            line = line.substring(1);
            if (!line.isBlank()) {
                list.set(0, line);
            } else {
                list.remove(0);
            }
        }
        line = list.get(list.size() - 1);
        if (line.endsWith("]")) {
            line = line.substring(0, line.length() - 1);
            if (!line.isBlank()) {
                list.set(list.size() - 1, line);
            } else {
                list.remove(list.size() - 1);
            }
        }

        var json = list.iterator();
        List<Document> stages = new ArrayList<>();
        String current = "";
        while (json.hasNext()) {
            while (current.isBlank() || !balanced(current)) {
                var next = json.next();
                if (!next.trim().isBlank()) {
                    current += next;
                }
            }
            try {
                stages.add(Document.parse(current));
            } catch (BsonInvalidOperationException e) {
                throw new BsonInvalidOperationException("Failed to parse:\n" + current, e);
            }
            current = "";
        }

        return stages;
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

        return open == close;
    }

    @NonNull
    protected <D> List<D> runQuery(@NonNull String queryTemplate, @NonNull MorphiaQuery<D> query, @NonNull FindOptions options) {
        String queryName = format("%s/%s/query.json", prefix(), queryTemplate);
        try {

            InputStream stream = getClass().getResourceAsStream(queryName);
            assertNotNull(stream, "Could not find query template: " + queryName);
            Document expectedQuery;
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                expectedQuery = Document.parse(new BufferedReader(reader).readLine());
            }

            assertDocumentEquals(query.toDocument(), expectedQuery);

            try (var cursor = query.iterator(options)) {
                return cursor.toList();
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected String discoverResourceName(StackTraceElement[] stackTrace) {
        String methodName = Arrays.stream(stackTrace)
                .filter(e -> isTestMethod(e))
                .findFirst()
                .get().getMethodName();
        if (methodName.startsWith("test")) {
            methodName = methodName.substring(4);
            methodName = methodName.substring(0, 1).toLowerCase() + methodName.substring(1);
        }
        return methodName;
    }

    private boolean isTestMethod(StackTraceElement element) {
        try {
            Class<?> klass = Class.forName(element.getClassName());
            Method method = klass.getDeclaredMethod(element.getMethodName());

            return method.getAnnotation(Test.class) != null;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private <D> List<D> map(Class<D> entityClass, List<Document> documents) {
        var codec = getDs().getCodecRegistry().get(entityClass);

        DecoderContext context = DecoderContext.builder().build();
        return documents.stream()
                .map(document -> {
                    return codec.decode(new DocumentReader(document), context);
                })
                .collect(toList());
    }
}
