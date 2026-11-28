package dev.morphia.test.query.updates;

import dev.morphia.UpdateOptions;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.updates.UpdateOperators.inc;

public class TestPositionalAll extends TemplatedTestBase {

    @Test
    @DisplayName("Update All Documents in an Array")
    public void testUpdateAllDocumentsInAnArray() {
        testUpdate((query) -> query,
                inc("grades.$[].std", -2));
    }

    @Test
    @DisplayName("Update All Elements in an Array")
    public void testUpdateAllElementsInAnArray() {
        testUpdate((query) -> query,
                inc("grades.$[]", 10));
    }

    /**
     * test data: dev/morphia/test/query/updates/positionalAll/updateNestedArraysInConjunctionWithIdentifier
     */
    @Test
    @DisplayName("Update Nested Arrays in Conjunction with $[<identifier>]")
    public void testUpdateNestedArraysInConjunctionWithIdentifier() {
        testUpdate((query) -> query, new UpdateOptions().arrayFilter(gte("score", 8)),
                inc("grades.$[].questions.$[score]", 2));
    }
}
