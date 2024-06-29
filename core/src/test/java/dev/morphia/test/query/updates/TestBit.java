package dev.morphia.test.query.updates;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.and;
import static dev.morphia.query.updates.UpdateOperators.or;
import static dev.morphia.query.updates.UpdateOperators.xor;

public class TestBit extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/bit/example1
     */
    @Test(testName = "Bitwise AND")
    public void testExample1() {
        testUpdate((query) -> query.filter(eq("_id", 1)), and("expdata", 10));
    }

    /**
     * test data: dev/morphia/test/query/updates/bit/example2
     */
    @Test(testName = "Bitwise OR")
    public void testExample2() {
        testUpdate((query) -> query.filter(eq("_id", 2)), or("expdata", 5));
    }

    /**
     * test data: dev/morphia/test/query/updates/bit/example3
     */
    @Test(testName = "Bitwise XOR")
    public void testExample3() {
        testUpdate((query) -> query.filter(eq("_id", 3)), xor("expdata", 5));
    }
}