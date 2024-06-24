package dev.morphia.test.query.updates;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.mul;

public class TestMul extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/mul/example1
     */
    @Test(testName = "Multiply the Value of a Field")
    public void testExample1() {
        testUpdate((query) -> query.filter(
                eq("_id", 1)),
                mul("price", 1.25),
                mul("quantity", 2));
    }

    /**
     * test data: dev/morphia/test/query/updates/mul/example2
     */
    @Test(testName = "Apply ``$mul`` Operator to a Non-existing Field")
    public void testExample2() {
        testUpdate((query) -> query.filter(
                eq("_id", 2)),
                mul("price", 100.0));
    }

    /**
     * test data: dev/morphia/test/query/updates/mul/example3
     */
    @Test(testName = "Multiply Mixed Numeric Types")
    public void testExample3() {
        testUpdate((query) -> query.filter(
                eq("_id", 3)),
                mul("price", 5));
    }
}