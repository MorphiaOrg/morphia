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
import com.mongodb.lang.NonNull;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.AggregationImpl;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaQuery;
import dev.morphia.test.util.Comparanator;

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
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.bson.json.JsonWriterSettings.builder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public abstract class TemplatedTestBase extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(TemplatedTestBase.class);

    private static final JsonWriterSettings JSON_WRITER_SETTINGS = builder()
            .indent(true)
            .build();

    protected final ObjectMapper mapper = new ObjectMapper();
    protected boolean skipPipelineCheck = false;

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

    public void testPipeline(ServerVersion serverVersion,
            boolean removeIds,
            boolean orderMatters,
            Function<Aggregation<Document>, Aggregation<Document>> pipeline) {
        String collection = "aggtest";
        checkMinServerVersion(serverVersion);
        var resourceName = discoverResourceName(new Exception().getStackTrace());
        loadData(collection, "data.json");

        List<Document> documents = runPipeline(resourceName, pipeline.apply(getDs().aggregate(collection)));

        List<Document> actual = removeIds ? removeIds(documents) : documents;
        List<Document> expected = loadExpected(resourceName);

        Comparanator.of(null, actual, expected, orderMatters).compare();
    }

    private static String toString(List<Document> actual, String prefix) {
        return actual.stream()
                .map(c -> c.toString())
                .collect(joining("\n\t", prefix, ""));
    }

    public <D> void testQuery(MorphiaQuery<D> query, FindOptions options, boolean orderMatters) {
        var resourceName = discoverResourceName(new Exception().getStackTrace());

        loadData(getDs().getCollection(query.getEntityClass()).getNamespace().getCollectionName(), "data.json");

        List<D> actual = runQuery(resourceName, query, options);

        List<D> expected = map(query.getEntityClass(), loadExpected(resourceName));

        if (orderMatters) {
            assertEquals(actual, expected);
        } else {
            assertListEquals(actual, expected);
        }
    }

    protected void loadData(String collection, String fileName) {
        var resourceName = discoverResourceName(new Exception().getStackTrace());
        insert(collection, loadJson(format("%s/%s/%s", prefix(), resourceName, fileName)));
    }

    protected @NotNull List<Document> loadExpected(String resourceName) {
        return loadJson(format("%s/%s/expected.json", prefix(), resourceName));
    }

    protected @NotNull <T> List<T> loadExpected(Class<T> type, String resourceName) {
        return loadJson(type, format("%s/%s/expected.json", prefix(), resourceName));
    }

    @NotNull
    protected List<Document> loadJson(String name) {
        List<Document> data = new ArrayList<>();
        InputStream stream = getClass().getResourceAsStream(name);
        if (stream == null) {
            fail(format("missing data file: src/test/resources/%s/%s", getClass().getPackageName().replace('.', '/'), name));
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            while (reader.ready()) {
                String json = reader.readLine();
                try {
                    data.add(Document.parse(json));
                } catch (JsonParseException e) {
                    throw new JsonParseException(e.getMessage() + "\n" + json);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
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
        String pipelineName = format("%s/%s/pipeline.json", prefix(), pipelineTemplate);
        List<Document> pipeline = ((AggregationImpl) aggregation).pipeline();

        if (!skipPipelineCheck) {
            List<Document> target = loadPipeline(pipelineName);
            assertEquals(
                    pipeline.stream().map(d -> d.toJson(JSON_WRITER_SETTINGS)).collect(joining("\n, ", "[\n", "\n]")),
                    target.stream().map(d -> d.toJson(JSON_WRITER_SETTINGS)).collect(joining("\n, ", "[\n", "\n]")),
                    "Should generate the same pipeline");
        }

        try (var cursor = aggregation.execute(Document.class)) {
            return cursor.toList();
        }

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
            while (!balanced(current += json.next())) {
            }
            stages.add(Document.parse(current));
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
