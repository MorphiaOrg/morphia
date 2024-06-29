package dev.morphia.test.query.updates;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.unset;

public class TestUnset extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/unset/example1
     */
    @Test(testName = "main")
    public void testExample1() {
        testUpdate(new ActionTestOptions().removeIds(true).orderMatters(false),
                (query) -> query.filter(eq("sku", "unknown")), unset("quantity", "instock"));
    }
}