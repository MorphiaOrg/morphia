package dev.morphia.test.query.filters;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.elemMatch;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.lt;

public class TestElemMatch extends FilterTest {
    @Test(testName = "Element Match")
    public void testExample1() {
        testQuery((query) -> query.filter(
                elemMatch("results", gte(80), lt(85))));
    }

    @Test(testName = "Array of Embedded Documents")
    public void testExample2() {
        testQuery((query) -> query.filter(
                elemMatch("results", eq("product", "xyz"), gte("score", 8))));
    }

    @Test(testName = "Single Query Condition")
    public void testExample3() {
        testQuery((query) -> query.filter(
                elemMatch("results", eq("product", "xyz"))));
    }

}
