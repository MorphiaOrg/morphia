package dev.morphia.test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.query.Query;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.test.util.ActionTestOptions;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static java.lang.Character.toLowerCase;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.bson.json.JsonWriterSettings.builder;

/**
 * The base of the converted operator example tests. Each test's fixture directory holds only its {@code scenario.json},
 * found through the example bindings, and {@link OperatorCaseRunner} checks it; the test body supplies Morphia's side
 * of the action and the legacy options its scenario must agree with.
 */
@ExtendWith(TestCoverageExtension.class)
public abstract class TemplatedTestBase extends TestBase {
    public static final JsonWriterSettings JSON_WRITER_SETTINGS = builder()
            .indent(true)
            .build();

    protected static final String EXAMPLE_TEST_COLLECTION = "example_test";

    public TemplatedTestBase() {
    }

    protected static String toString(List<Document> actual, String prefix) {
        return actual.stream()
                .map(c -> c.toJson(JSON_WRITER_SETTINGS))
                .collect(joining("\n\t", prefix, ""));
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
        new OperatorCaseRunner(this).aggregate(scenario(), options, pipeline);
    }

    public void testQuery(Function<Query<Document>, Query<Document>> function) {
        testQuery(new ActionTestOptions(), function);
    }

    public void testQuery(ActionTestOptions options,
            Function<Query<Document>, Query<Document>> function) {
        new OperatorCaseRunner(this).find(scenario(), options, function);
    }

    public void testUpdate(Function<Query<Document>, Query<Document>> function, UpdateOperator... operators) {
        testUpdate(new ActionTestOptions(), function, operators);
    }

    public void testUpdate(ActionTestOptions options,
            Function<Query<Document>, Query<Document>> function, UpdateOperator... operators) {
        new OperatorCaseRunner(this).update(scenario(), options, function, operators);
    }

    /**
     * A test that loaded an extra collection still names it: its scenario seeds every documented collection, so this
     * only checks that the scenario seeds it.
     */
    protected void loadData(String collection, int index) {
        OperatorCase scenario = scenario();
        if (!scenario.scenario().getDocument("collections").containsKey(collection)) {
            Assertions.fail(scenario.resource() + " does not seed the collection " + collection);
        }
    }

    protected String discoverResourceName() {
        var method = findTestMethod();
        String bound = ExampleBindings.resource(method);
        if (bound != null) {
            return bound;
        }
        String methodName = method.getName();

        if (methodName.startsWith("test")) {
            methodName = methodName.substring(4);
            methodName = methodName.substring(0, 1).toLowerCase() + methodName.substring(1);
        }
        return methodName;
    }

    protected Method findTestMethod() {
        return stream(new Exception().getStackTrace())
                .map(this::isTestMethod)
                .filter(Objects::nonNull)
                .findFirst()
                .get();
    }

    /**
     * @return the absolute resource of a converted case's scenario for the fixture {@code resourceName}
     */
    protected String scenarioResource(String resourceName) {
        return "/%s/%s/%s/scenario.json".formatted(getClass().getPackageName().replace('.', '/'), prefix(), resourceName);
    }

    public final String prefix() {
        String root = getClass().getSimpleName().replace("Test", "");
        return toLowerCase(root.charAt(0)) + root.substring(1);
    }

    /**
     * @return the calling test's scenario; every operator example test has one
     */
    private OperatorCase scenario() {
        String scenario = scenarioResource(discoverResourceName());
        if (getClass().getResource(scenario) == null) {
            Assertions.fail("No scenario at " + scenario + ": every operator example runs on the operator case runner");
        }
        return OperatorCase.of(scenario);
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
}
