package dev.morphia.test.query.filters;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.mongodb.client.result.UpdateResult;

import dev.morphia.UpdateOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaQuery;
import dev.morphia.query.Query;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.aggregation.model.Martian;
import dev.morphia.test.models.User;
import dev.morphia.test.util.ActionTestOptions;
import dev.morphia.test.util.Comparanator;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.testng.Assert.assertEquals;

public class FilterTest extends TemplatedTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(FilterTest.class);

    public FilterTest() {
        super(buildConfig(Martian.class, User.class)
                .applyIndexes(true)
                .codecProvider(new ZDTCodecProvider()));
    }

    public void testQuery(Function<Query<Document>, Query<Document>> function) {
        testQuery(new ActionTestOptions(), function);
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
        testUpdate(new ActionTestOptions(), function, operators);
    }

    public void testUpdate(ActionTestOptions options,
            Function<Query<Document>, Query<Document>> function, UpdateOperator... operators) {

        var resourceName = prepareDatabase(options);

        Query<Document> apply = function.apply(getDs().find(EXAMPLE_TEST_COLLECTION, Document.class)
                .disableValidation());
        List<Document> actual = runUpdate(options, resourceName, apply, options.findOptions(), operators);

        checkExpected(options, resourceName, actual);
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

    private void validateTestName(String resourceName) {
        Method method = findTestMethod();
        Test test = method.getAnnotation(Test.class);
        assertEquals(
                test.testName(), loadTestName(resourceName),
                "%s#%s does not have a name configured on the test.".formatted(method.getDeclaringClass().getName(),
                        method.getName()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<Document> runQuery(ActionTestOptions testOptions, String pipelineTemplate, Query<Document> query, FindOptions options) {
        String resourceName = format("%s/%s/action.json", prefix(), pipelineTemplate);
        Document document = ((MorphiaQuery) query).toDocument();

        if (!skipActionCheck && !testOptions.skipActionCheck()) {
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<Document> runUpdate(ActionTestOptions testOptions, String pipelineTemplate, Query<Document> query,
            FindOptions options, UpdateOperator... operators) {
        String resourceName = format("%s/%s/action.json", prefix(), pipelineTemplate);
        Document document = ((MorphiaQuery) query).toDocument();

        List<Document> action = loadAction(resourceName);
        if (!skipActionCheck && !testOptions.skipActionCheck()) {
            assertEquals(toJson(document), toJson(action.get(0)), "Should generate the same query document");
        }

        var first = operators[0];
        var others = Arrays.copyOfRange(operators, 1, operators.length);
        if (!testOptions.skipDataCheck()) {
            var resource = loadResource(resourceName);
            UpdateResult update = query.update(new UpdateOptions().multi(resource.contains("updateMany")), first, others);
            try (var cursor = getDs().find(EXAMPLE_TEST_COLLECTION, Document.class).iterator()) {
                return cursor.toList();
            }
        } else {
            return emptyList();
        }
    }

}
