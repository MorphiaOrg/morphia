package dev.morphia.test.query.updates;

import java.util.List;

import dev.morphia.UpdateOptions;
import dev.morphia.test.TemplatedTestBase;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.ne;
import static dev.morphia.query.updates.UpdateOperators.inc;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestPositionalFiltered extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/updates/positionalFiltered/updateAllArrayElementsThatMatchArrayFilters
     */
    @Test
    @DisplayName("Update All Array Elements That Match arrayFilters")
    public void testUpdateAllArrayElementsThatMatchArrayFilters() {
        testUpdate((query) -> query, new UpdateOptions().arrayFilter(gte("element", 100)),
                set("grades.$[element]", 100));
    }

    /**
     * test data: dev/morphia/test/query/updates/positionalFiltered/updateAllDocumentsThatMatchArrayFiltersInAnArray
     */
    @Test
    @DisplayName("Update All Documents That Match arrayFilters in an Array")
    public void testUpdateAllDocumentsThatMatchArrayFiltersInAnArray() {
        testUpdate((query) -> query, new UpdateOptions().arrayFilter(gte("elem.grade", 85)),
                set("grades.$[elem].mean", 100));
    }

    /**
     * test data: dev/morphia/test/query/updates/positionalFiltered/updateAllArrayElementsThatMatchMultipleConditions
     */
    @Test
    @DisplayName("Update All Array Elements that Match Multiple Conditions")
    public void testUpdateAllArrayElementsThatMatchMultipleConditions() {
        // arrayFilter(Filter) adds one filter per condition; the server takes one filter per identifier.
        testUpdate((query) -> query,
                new UpdateOptions().arrayFilters(List.of(new Document("elem.grade", new Document("$gte", 80))
                        .append("elem.std", new Document("$gte", 5)))),
                inc("grades.$[elem].std", -1));
    }

    /**
     * test data: dev/morphia/test/query/updates/positionalFiltered/updateArrayElementsUsingANegationOperator
     */
    @Test
    @DisplayName("Update Array Elements Using a Negation Operator")
    public void testUpdateArrayElementsUsingANegationOperator() {
        testUpdate((query) -> query, new UpdateOptions().arrayFilter(ne("degree.level", "Bachelor")),
                set("degrees.$[degree].gradcampaign", 1));
    }

    /**
     * test data: dev/morphia/test/query/updates/positionalFiltered/updateNestedArraysInConjunctionWith
     */
    @Test
    @DisplayName("Update Nested Arrays in Conjunction with $[]")
    public void testUpdateNestedArraysInConjunctionWith() {
        testUpdate((query) -> query, new UpdateOptions().arrayFilter(eq("t.type", "quiz")).arrayFilter(gte("score", 8)),
                inc("grades.$[t].questions.$[score]", 2));
    }
}
