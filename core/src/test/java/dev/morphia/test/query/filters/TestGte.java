package dev.morphia.test.query.filters;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.gte;

public class TestGte extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/gte/example1
     */
    @Test(testName = "Match Document Fields")
    public void testExample1() {
        testQuery(new QueryTestOptions().removeIds(true), (query) -> query.filter(gte("quantity", 20)));
    }
}