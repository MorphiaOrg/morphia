package dev.morphia.test.query.updates;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.mul;

public class TestMul extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/mul/multiplyMixedNumericTypes
     */
    @Test
    @DisplayName("Multiply Mixed Numeric Types")
    public void testMultiplyMixedNumericTypes() {
        testUpdate((query) -> query.filter(eq("_id", 3)), mul("price", 5));
    }
}