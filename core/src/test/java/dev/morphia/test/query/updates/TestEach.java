package dev.morphia.test.query.updates;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.addToSet;
import static dev.morphia.query.updates.UpdateOperators.push;

public class TestEach extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/each/useEachWithPushOperator
     */
    @Test
    @DisplayName("Use ``$each``  with ``$push`` Operator")
    public void testUseEachWithPushOperator() {
        testUpdate((query) -> query.filter(eq("_id", 1)), push("scores", List.of(90, 92, 85)));
    }

    /**
     * test data: dev/morphia/test/query/updates/each/useEachWithAddToSetOperator
     */
    @Test
    @DisplayName("Use ``$each``  with ``$addToSet`` Operator")
    public void testUseEachWithAddToSetOperator() {
        testUpdate((query) -> query.filter(eq("_id", 2)),
                addToSet("tags", List.of("camera", "electronics", "accessories")));
    }
}