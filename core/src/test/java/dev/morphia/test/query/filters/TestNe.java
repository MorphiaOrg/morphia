package dev.morphia.test.query.filters;

import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.ne;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestNe extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/ne/example1
     */
    @Test(testName = "Match Document Fields That Are Not Equal")
    public void testExample1() {
        testQuery(new ActionTestOptions().removeIds(true),
                (query) -> query.filter(
                        ne("quantity", 20)));
    }

    /**
     * test data: dev/morphia/test/query/filters/ne/example2
     */
    @Test(testName = "Update Based on Not Equal Embedded Document Fields")
    public void testExample2() {
        testUpdate(new ActionTestOptions().removeIds(true),
                (query) -> query.filter(
                        ne("carrier.fee", 1)),
                set("price", 9.99));
    }
}