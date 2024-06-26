package dev.morphia.test.query.updates;

import java.util.List;

import dev.morphia.UpdateOptions;
import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.push;

public class TestPush extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/push/example1
     */
    @Test(testName = "Append a Value to an Array")
    public void testExample1() {
        testUpdate((query) -> query.filter(
                eq("_id", 1)),
                push("scores", 89));
    }

    /**
     * test data: dev/morphia/test/query/updates/push/example2
     */
    @Test(testName = "Append a Value to Arrays in Multiple Documents")
    public void testExample2() {
        UpdateOptions updateOptions = new UpdateOptions().multi(true);
        testUpdate((query) -> query.filter(),
                push("scores", 95));
    }

    /**
     * test data: dev/morphia/test/query/updates/push/example3
     */
    @Test(testName = "Append Multiple Values to an Array")
    public void testExample3() {
        testUpdate((query) -> query.filter(
                eq("_id", 1)),
                push("scores", List.of(90, 92, 85)));
    }

    /**
     * test data: dev/morphia/test/query/updates/push/example4
     */
    @Test(testName = "Use ``$push`` Operator with Multiple Modifiers")
    public void testExample4() {
        testUpdate((query) -> query.filter(
                eq("_id", 5)),
                push("quizzes", List.of(
                        document("wk", 5).field("score", 8),
                        document("wk", 6).field("score", 7),
                        document("wk", 7).field("score", 6)))
                        .sort(descending("score"))
                        .slice(3));
    }
}