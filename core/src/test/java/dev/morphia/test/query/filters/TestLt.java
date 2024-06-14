package dev.morphia.test.query.filters;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.lt;

public class TestLt extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/lt/example1
     */
    @Test(testName = "Match Document Fields")
    public void testExample1() {
        testQuery(new QueryTestOptions().removeIds(true),
                (query) -> query.filter(
                        lt("quantity", 20)));
    }
}