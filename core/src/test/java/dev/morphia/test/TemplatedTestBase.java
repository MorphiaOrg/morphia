package dev.morphia.test;

import java.io.File;
import java.util.function.Function;

import dev.morphia.UpdateOptions;
import dev.morphia.aggregation.Aggregation;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.updates.UpdateOperator;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.junit.jupiter.api.extension.ExtendWith;

import static java.lang.Character.toLowerCase;

/**
 * The base of the operator example tests. A test's fixture is found from its method name: {@code testRegexFlags}
 * reads {@code <operator>/regexFlags/scenario.json}, which {@link OperatorCaseRunner} checks the test against. The
 * scenario holds everything about the run (its seed data, indexes, comparison and server requirements), so a test
 * supplies only its Morphia call.
 */
@ExtendWith(CurrentTest.class)
public abstract class TemplatedTestBase extends TestBase {
    public static final JsonWriterSettings JSON_WRITER_SETTINGS = JsonWriterSettings.builder()
            .indent(true)
            .build();

    /** The collection a scenario names when its page names none. */
    protected static final String EXAMPLE_TEST_COLLECTION = "example_test";

    /**
     * @param path a path relative to the core module
     * @return that file
     */
    public static File rootToCore(String path) {
        return new File(CORE_ROOT, path);
    }

    public void testPipeline(Function<Aggregation<Document>, Aggregation<Document>> pipeline) {
        new OperatorCaseRunner(this).aggregate(scenario(), pipeline);
    }

    public void testQuery(Function<Query<Document>, Query<Document>> function) {
        new OperatorCaseRunner(this).find(scenario(), function);
    }

    /**
     * @param function the query
     * @param options  the find options carrying the documented projection
     */
    public void testQuery(Function<Query<Document>, Query<Document>> function, FindOptions options) {
        new OperatorCaseRunner(this).find(scenario(), function, options);
    }

    public void testUpdate(Function<Query<Document>, Query<Document>> function, UpdateOperator... operators) {
        new OperatorCaseRunner(this).update(scenario(), function, operators);
    }

    /**
     * @param function  the query
     * @param options   the update options carrying the documented arrayFilters and upsert
     * @param operators the update operators
     */
    public void testUpdate(Function<Query<Document>, Query<Document>> function, UpdateOptions options,
            UpdateOperator... operators) {
        new OperatorCaseRunner(this).update(scenario(), function, options, operators);
    }

    public final String prefix() {
        String root = getClass().getSimpleName().replace("Test", "");
        return toLowerCase(root.charAt(0)) + root.substring(1);
    }

    /**
     * @return the running test's fixture folder: its method name without {@code test}, first letter lowered
     */
    protected String discoverResourceName() {
        String methodName = CurrentTest.method().getName();
        if (methodName.startsWith("test")) {
            methodName = methodName.substring(4);
            methodName = methodName.substring(0, 1).toLowerCase() + methodName.substring(1);
        }
        return methodName;
    }

    /**
     * @param resourceName a fixture's folder name
     * @return the absolute resource of that fixture's {@code scenario.json}
     */
    protected String scenarioResource(String resourceName) {
        return "/%s/%s/%s/scenario.json".formatted(getClass().getPackageName().replace('.', '/'), prefix(), resourceName);
    }

    private OperatorCase scenario() {
        return OperatorCase.of(scenarioResource(discoverResourceName()));
    }
}
