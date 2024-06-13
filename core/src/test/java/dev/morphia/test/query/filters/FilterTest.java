package dev.morphia.test.query.filters;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

import dev.morphia.query.MorphiaQuery;
import dev.morphia.query.Query;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.aggregation.model.Martian;
import dev.morphia.test.models.User;
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
        testQuery(new QueryTestOptions(), function);
    }

    public void testQuery(QueryTestOptions options,
            Function<Query<Document>, Query<Document>> function) {

        checkMinServerVersion(options.serverVersion());
        checkMinDriverVersion(minDriver);
        var resourceName = discoverResourceName();
        validateTestName(resourceName);
        loadData(resourceName, AGG_TEST_COLLECTION);
        loadIndex(resourceName, AGG_TEST_COLLECTION);

        List<Document> actual = runQuery(resourceName, function.apply(getDs().find(AGG_TEST_COLLECTION, Document.class)
                .disableValidation()));

        if (!skipDataCheck) {
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

    private void validateTestName(String resourceName) {
        Method method = findTestMethod();
        Test test = method.getAnnotation(Test.class);
        assertEquals(
                test.testName(), loadTestName(resourceName),
                "%s#%s does not have a name configured on the test.".formatted(method.getDeclaringClass().getName(),
                        method.getName()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<Document> runQuery(String pipelineTemplate, Query<Document> query) {
        String resourceName = format("%s/%s/action.json", prefix(), pipelineTemplate);
        Document document = ((MorphiaQuery) query).toDocument();

        if (!skipActionCheck) {
            Document target = loadQuery(resourceName);
            assertEquals(toJson(document), toJson(target), "Should generate the same query document");
        }

        if (!skipDataCheck) {
            try (var cursor = query.iterator()) {
                return cursor.toList();
            }
        } else {
            return emptyList();
        }
    }

}
