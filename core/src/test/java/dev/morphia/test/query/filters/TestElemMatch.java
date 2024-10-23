package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.elemMatch;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.lt;

public class TestElemMatch extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/elemMatch/example1
     * 
     */
    @Test(testName = "Element Match")
    public void testExample1() {
        testQuery((query) -> query.filter(elemMatch("results", gte(80), lt(85))));
    }

    /**
     * test data: dev/morphia/test/query/filters/elemMatch/example2
     * 
     */
    @Test(testName = "Array of Embedded Documents")
    public void testExample2() {
        testQuery((query) -> query.filter(elemMatch("results", eq("product", "xyz"), gte("score", 8))));
    }

    /**
     * test data: dev/morphia/test/query/filters/elemMatch/example3
     * 
     */
    @Test(testName = "Single Query Condition")
    public void testExample3() {
        testQuery((query) -> query.filter(elemMatch("results", eq("product", "xyz"))));
    }

}
