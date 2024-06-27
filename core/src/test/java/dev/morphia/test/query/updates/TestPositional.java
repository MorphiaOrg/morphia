package dev.morphia.test.query.updates;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.elemMatch;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.lte;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestPositional extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/positional/example1
     */
    @Test(testName = "Update Values in an Array")
    public void testExample1() {
        testUpdate(new ActionTestOptions().skipDataCheck(true),
                (query) -> query.filter(
                        eq("_id", 1),
                        eq("grades", 80)),
                set("grades.$", 82));
    }

    /**
     * test data: dev/morphia/test/query/updates/positional/example2
     */
    @Test(testName = "Update Documents in an Array")
    public void testExample2() {
        testUpdate((query) -> query.filter(
                eq("_id", 4),
                eq("grades.grade", 85)),
                set("grades.$.std", 6));
    }

    /**
     * test data: dev/morphia/test/query/updates/positional/example3
     */
    @Test(testName = "Update Embedded Documents Using Multiple Field Matches")
    public void testExample3() {
        testUpdate((query) -> query.filter(
                eq("_id", 5),
                elemMatch("grades", lte("grade", 90), gt("mean", 80))),
                set("grades.$.std", 6));
    }

    /**
     * test data: dev/morphia/test/query/updates/positional/example4
     */
    @Test(testName = "Update with Multiple Array Matches")
    public void testExample4() {
        testUpdate((query) -> query.filter(
                eq("activity_ids", 1),
                eq("grades", 95),
                eq("deans_list", 2021)),
                set("deans_list.$", 2022));
    }

    /**
     * test data: dev/morphia/test/query/updates/positional/example5
     */
    @Test(testName = "Update Nested Arrays in Conjunction with ``$[]``")
    public void testExample5() {
        // docs out of date?
    }
}