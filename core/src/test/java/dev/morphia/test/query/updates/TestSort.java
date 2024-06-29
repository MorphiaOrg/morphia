package dev.morphia.test.query.updates;

import java.util.List;
import java.util.Map;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.*;
import static dev.morphia.query.updates.UpdateOperators.push;

public class TestSort extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/sort/example1
     */
    @Test(testName = "Sort Array of Documents by a Field in the Documents")
    public void testExample1() {
        // the Map order throws off structural checks
        testUpdate(new ActionTestOptions().skipActionCheck(true),
                (query) -> query.filter(
                        eq("_id", 1)),
                push("quizzes", List.of(
                        Map.of("id", 3, "score", 8),
                        Map.of("id", 4, "score", 7),
                        Map.of("id", 5, "score", 6)))
                        .sort(ascending("score")));
    }

    /**
     * test data: dev/morphia/test/query/updates/sort/example2
     */
    @Test(testName = "Sort Array Elements That Are Not Documents")
    public void testExample2() {
        testUpdate((query) -> query.filter(
                eq("_id", 2)),
                push("tests", List.of(40, 60))
                        .sort(1));
    }

    /**
     * test data: dev/morphia/test/query/updates/sort/example3
     */
    @Test(testName = "Update Array Using Sort Only")
    public void testExample3() {
        testUpdate((query) -> query.filter(
                eq("_id", 3)),
                push("tests", List.of())
                        .sort(-1));
    }

    /**
     * test data: dev/morphia/test/query/updates/sort/example4
     */
    @Test(testName = "Use ``$sort`` with Other ``$push`` Modifiers")
    public void testExample4() {
        // the Map order throws off structural checks
        testUpdate(new ActionTestOptions().skipActionCheck(true),
                (query) -> query.filter(
                        eq("_id", 5)),
                push("quizzes", List.of(
                        Map.of("wk", 5, "score", 8),
                        Map.of("wk", 6, "score", 7),
                        Map.of("wk", 7, "score", 6)))
                        .sort(descending("score"))
                        .slice(3));
    }
}