package dev.morphia.test.aggregation.expressions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.lang.NonNull;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.AggregationImpl;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.test.TestBase;

import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.jetbrains.annotations.NotNull;

import static java.lang.Character.toLowerCase;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.bson.json.JsonWriterSettings.builder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public abstract class TemplatedTestBase extends TestBase {
    protected final ObjectMapper mapper = new ObjectMapper();

    public TemplatedTestBase() {
    }

    public TemplatedTestBase(MorphiaConfig config) {
        super(config);
    }

    public final String prefix() {
        String root = getClass().getSimpleName().substring(4);
        return toLowerCase(root.charAt(0)) + root.substring(1);
    }

    public void testPipeline(double serverVersion, String resourceNamed, boolean removeIds, boolean orderMatters,
            Function<Aggregation<Document>, Aggregation<Document>> pipeline) {
        String collection = "aggtest";
        checkMinServerVersion(serverVersion);
        var resourceName = discoverResourceName(new Exception().getStackTrace());
        loadData(collection, resourceName);

        List<Document> documents = runPipeline(resourceName, pipeline.apply(getDs().aggregate(collection)));

        List<Document> actual = removeIds ? removeIds(documents) : documents;
        List<Document> expected = loadExpected(resourceName);

        if (orderMatters) {
            assertEquals(actual, expected);
        } else {
            assertListEquals(actual, expected);
        }
    }

    public <D> void testQuery(Query<D> query, FindOptions options, boolean orderMatters) {
        var resourceName = discoverResourceName(new Exception().getStackTrace());

        loadData(getDs().getCollection(query.getEntityClass()).getNamespace().getCollectionName(), resourceName);

        List<D> actual = runQuery(resourceName, query, options);

        List<D> expected = map(query.getEntityClass(), loadExpected(resourceName));

        if (orderMatters) {
            assertEquals(actual, expected);
        } else {
            assertListEquals(actual, expected);
        }
    }

    protected void loadData(String collection, String resourceName) {
        insert(collection, loadJson(format("%s/%s/data.json", prefix(), resourceName)));
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
            fail("missing data file: " + name);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            while (reader.ready()) {
                data.add(Document.parse(reader.readLine()));
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
        try {
            List<Document> pipeline = ((AggregationImpl) aggregation).pipeline();

            Iterator<Map<String, Object>> iterator = mapper.readValue(getClass().getResourceAsStream(pipelineName), List.class)
                    .iterator();
            for (Document stage : pipeline) {
                Object next = iterator.next();
                assertEquals(mapper.readValue(stage.toJson(), Map.class), next,
                        pipeline.stream()
                                .map(d -> d.toJson(builder()
                                        .indent(true)
                                        .build()))
                                .collect(Collectors.joining("\n", "[\n", "\n]")));
            }

            try (var cursor = aggregation.execute(Document.class)) {
                return cursor.toList();
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    @NonNull
    protected <D> List<D> runQuery(@NonNull String queryTemplate, @NonNull Query<D> query,
            @NonNull FindOptions options) {
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

    private String discoverResourceName(StackTraceElement[] stackTrace) {
        String methodName = Arrays.stream(stackTrace)
                .filter(e -> !(e.getMethodName().equals("testPipeline") ||
                        e.getMethodName().equals("testQuery")))
                .findFirst()
                .get().getMethodName();
        if (methodName.startsWith("test")) {
            methodName = methodName.substring(4);
            methodName = methodName.substring(0, 1).toLowerCase() + methodName.substring(1);
        }
        return methodName;
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
