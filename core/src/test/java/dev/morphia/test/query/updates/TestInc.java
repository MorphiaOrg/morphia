package dev.morphia.test.query.updates;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.inc;

public class TestInc extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/inc/example1
     */
    @Test
    @DisplayName("main")
    public void testExample1() {
        testUpdate((query) -> query.filter(eq("sku", "abc123")), inc("quantity", -2), inc("metrics.orders", 1));
    }
}