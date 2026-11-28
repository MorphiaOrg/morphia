package dev.morphia.test.query.updates;

import java.util.Map;

import dev.morphia.UpdateOptions;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.set;
import static dev.morphia.query.updates.UpdateOperators.setOnInsert;

public class TestSetOnInsert extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/updates/setOnInsert/setFieldsOnlyOnInsert
     */
    @Test
    @DisplayName("Set Fields Only on Insert")
    public void testSetFieldsOnlyOnInsert() {
        testUpdate((query) -> query.filter(eq("_id", 1)), new UpdateOptions().upsert(true),
                set("item", "apple"), setOnInsert(Map.of("defaultQty", 100)));
    }
}
