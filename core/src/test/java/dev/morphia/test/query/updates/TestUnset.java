package dev.morphia.test.query.updates;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.unset;

public class TestUnset extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/unset/unsetUpdateOperator
     */
    @Test
    @DisplayName("main")
    public void testUnsetUpdateOperator() {
        testUpdate(new ActionTestOptions().removeIds(true).orderMatters(false),
                (query) -> query.filter(eq("sku", "unknown")), unset("quantity", "instock"));
    }
}