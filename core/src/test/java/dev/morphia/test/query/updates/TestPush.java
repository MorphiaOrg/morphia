package dev.morphia.test.query.updates;

import java.util.List;

import dev.morphia.UpdateOptions;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.push;

public class TestPush extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/push/appendAValueToAnArray
     */
    @Test
    @DisplayName("Append a Value to an Array")
    public void testAppendAValueToAnArray() {
        testUpdate((query) -> query.filter(eq("_id", 1)), push("scores", 89));
    }

    /**
     * test data: dev/morphia/test/query/updates/push/appendAValueToArraysInMultipleDocuments
     */
    @Test
    @DisplayName("Append a Value to Arrays in Multiple Documents")
    public void testAppendAValueToArraysInMultipleDocuments() {
        UpdateOptions updateOptions = new UpdateOptions().multi(true);
        testUpdate((query) -> query.filter(), push("scores", 95));
    }

    /**
     * test data: dev/morphia/test/query/updates/push/appendMultipleValuesToAnArray
     */
    @Test
    @DisplayName("Append Multiple Values to an Array")
    public void testAppendMultipleValuesToAnArray() {
        testUpdate((query) -> query.filter(eq("_id", 1)), push("scores", List.of(90, 92, 85)));
    }

    /**
     * test data: dev/morphia/test/query/updates/push/usePushOperatorWithMultipleModifiers
     */
    @Test
    @DisplayName("Use ``$push`` Operator with Multiple Modifiers")
    public void testUsePushOperatorWithMultipleModifiers() {
        testUpdate((query) -> query.filter(eq("_id", 5)),
                push("quizzes", List.of(document("wk", 5).field("score", 8), document("wk", 6).field("score", 7),
                        document("wk", 7).field("score", 6))).sort(descending("score")).slice(3));
    }
}