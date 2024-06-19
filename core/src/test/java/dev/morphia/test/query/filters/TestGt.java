package dev.morphia.test.query.filters;

import dev.morphia.query.updates.UpdateOperators;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.gt;

public class TestGt extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/gt/example1
     */
    @Test(testName = "Match Document Fields")
    public void testExample1() {
        testQuery(new ActionTestOptions().removeIds(true), (query) -> query.filter(gt("quantity", 20)));
    }

    /**
     * test data: dev/morphia/test/query/filters/gt/example2
     */
    @Test(testName = "Perform an Update Based on Embedded Document Fields")
    public void testExample2() {
        testUpdate(new ActionTestOptions().removeIds(true), (query) -> query.filter(gt("carrier.fee", 2)),
                UpdateOperators.set("price", 9.99));
    }
}