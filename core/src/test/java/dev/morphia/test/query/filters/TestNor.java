package dev.morphia.test.query.filters;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.exists;
import static dev.morphia.query.filters.Filters.lt;
import static dev.morphia.query.filters.Filters.nor;

public class TestNor extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/nor/example1
     */
    @Test(testName = "``$nor`` Query with Two Expressions")
    public void testExample1() {
        testQuery(new QueryTestOptions().skipDataCheck(true),
                (query) -> query.filter(nor(eq("price", 1.99), eq("sale", true))));
    }

    /**
     * test data: dev/morphia/test/query/filters/nor/example2
     */
    @Test(testName = "``$nor`` and Additional Comparisons")
    public void testExample2() {
        testQuery(new QueryTestOptions().skipDataCheck(true),
                (query) -> query.filter(nor(eq("price", 1.99), lt("qty", 20), eq("sale", true))));
    }

    /**
     * test data: dev/morphia/test/query/filters/nor/example3
     */
    @Test(testName = "``$nor`` and ``$exists``")
    public void testExample3() {
        testQuery(new QueryTestOptions().skipDataCheck(true), (query) -> query
                .filter(nor(eq("price", 1.99), exists("price").not(), eq("sale", true), exists("sale").not())

                ));
    }
}