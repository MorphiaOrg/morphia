package dev.morphia.test.query.filters;

import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.lt;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestLt extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/lt/example1
     */
    @Test(testName = "Match Document Fields")
    public void testExample1() {
        testQuery(new ActionTestOptions().removeIds(true), (query) -> query.filter(lt("quantity", 20)));
    }

    /**
     * test data: dev/morphia/test/query/filters/lt/example2
     */
    @Test(testName = "Perform an Update Based on Embedded Document Fields")
    public void testExample2() {
        testUpdate(new ActionTestOptions().removeIds(true),
                (query) -> query.filter(
                        lt("carrier.fee", 20)),
                set("price", 9.99));
    }
}