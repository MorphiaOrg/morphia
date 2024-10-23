package dev.morphia.test.query.updates;

import java.util.List;
import java.util.Map;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.push;

public class TestSlice extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/slice/example1
     */
    @Test(testName = "Slice from the End of the Array")
    public void testExample1() {
        testUpdate((query) -> query.filter(eq("_id", 1)), push("scores", List.of(80, 78, 86)).slice(-5));
    }

    /**
     * test data: dev/morphia/test/query/updates/slice/example2
     */
    @Test(testName = "Slice from the Front of the Array")
    public void testExample2() {
        testUpdate((query) -> query.filter(eq("_id", 2)), push("scores", List.of(100, 20)).slice(3));
    }

    /**
     * test data: dev/morphia/test/query/updates/slice/example3
     */
    @Test(testName = "Update Array Using Slice Only")
    public void testExample3() {
        testUpdate((query) -> query.filter(eq("_id", 3)), push("scores", List.of()).slice(-3));
    }

    /**
     * test data: dev/morphia/test/query/updates/slice/example4
     */
    @Test(testName = "Use ``$slice`` with Other ``$push`` Modifiers")
    public void testExample4() {
        // the Map order throws off structural checks
        testUpdate(new ActionTestOptions().skipActionCheck(true), (query) -> query.filter(eq("_id", 5)),
                push("quizzes",
                        List.of(Map.of("wk", 5, "score", 8), Map.of("wk", 6, "score", 7), Map.of("wk", 7, "score", 6)))
                        .sort(descending("score")).slice(3));
    }
}