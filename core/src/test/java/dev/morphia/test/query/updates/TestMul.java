package dev.morphia.test.query.updates;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.mul;

public class TestMul extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/mul/multiplyTheValueOfAField
     */
    @Test
    @DisplayName("Multiply the Value of a Field")
    public void testMultiplyTheValueOfAField() {
        testUpdate((query) -> query.filter(eq("_id", 1)), mul("price", 1.25), mul("quantity", 2));
    }

    /**
     * test data: dev/morphia/test/query/updates/mul/applyMulOperatorToANonExistingField
     */
    @Test
    @DisplayName("Apply ``$mul`` Operator to a Non-existing Field")
    public void testApplyMulOperatorToANonExistingField() {
        testUpdate((query) -> query.filter(eq("_id", 2)), mul("price", 100.0));
    }

    /**
     * test data: dev/morphia/test/query/updates/mul/multiplyMixedNumericTypes
     */
    @Test
    @DisplayName("Multiply Mixed Numeric Types")
    public void testMultiplyMixedNumericTypes() {
        testUpdate((query) -> query.filter(eq("_id", 3)), mul("price", 5));
    }
}