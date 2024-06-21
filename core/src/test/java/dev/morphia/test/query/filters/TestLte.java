package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.lte;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestLte extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/lte/example1
     */
    @Test(testName = "Match Document Fields")
    public void testExample1() {
        testQuery(new ActionTestOptions().removeIds(true), (query) -> query.filter(lte("quantity", 20)));
    }

    /**
     * test data: dev/morphia/test/query/filters/lte/example2
     */
    @Test(testName = "Perform an Update Based on Embedded Document Fields")
    public void testExample2() {
        testUpdate(new ActionTestOptions().removeIds(true), (query) -> query.filter(lte("carrier.fee", 5)),
                set("price", 9.99));
    }
}