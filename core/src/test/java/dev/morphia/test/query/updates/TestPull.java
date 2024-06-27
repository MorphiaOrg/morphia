package dev.morphia.test.query.updates;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.elemMatch;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.in;
import static dev.morphia.query.updates.UpdateOperators.pull;

public class TestPull extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/pull/example1
     */
    @Test(testName = "Remove All Items That Equal a Specified Value")
    public void testExample1() {
        testUpdate((query) -> query.filter(), pull("fruits", in(List.of("apples", "oranges"))),
                pull("vegetables", "carrots"));
    }

    /**
     * test data: dev/morphia/test/query/updates/pull/example2
     */
    @Test(testName = "Remove All Items That Match a Specified ``$pull`` Condition")
    public void testExample2() {
        testUpdate((query) -> query.filter(eq("_id", 1)), pull("votes", gte(6)));
    }

    /**
     * test data: dev/morphia/test/query/updates/pull/example3
     */
    @Test(testName = "Remove All Items That Match a Specified ``$pull`` Condition With :method:`~db.collection.bulkWrite()`", enabled = false, description = "test is irrelevant to Morphia")
    public void testExample3() {

    }

    /**
     * test data: dev/morphia/test/query/updates/pull/example4
     */
    @Test(testName = "Remove Items from an Array of Documents")
    public void testExample4() {
        testUpdate((query) -> query.filter(), pull("results", eq("score", 8), eq("item", "B")));
    }

    /**
     * test data: dev/morphia/test/query/updates/pull/example5
     */
    @Test(testName = "Remove Documents from Nested Arrays")
    public void testExample5() {
        testUpdate((query) -> query.filter(), pull("results", elemMatch("answers", eq("q", 2), gte("a", 8))));
    }
}