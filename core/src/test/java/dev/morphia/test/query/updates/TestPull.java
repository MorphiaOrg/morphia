package dev.morphia.test.query.updates;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.in;
import static dev.morphia.query.updates.UpdateOperators.pull;

public class TestPull extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/pull/removeAllItemsThatEqualASpecifiedValue
     */
    @Test
    @DisplayName("Remove All Items That Equal a Specified Value")
    public void testRemoveAllItemsThatEqualASpecifiedValue() {
        testUpdate((query) -> query.filter(), pull("fruits", in(List.of("apples", "oranges"))),
                pull("vegetables", "carrots"));
    }

    /**
     * test data: dev/morphia/test/query/updates/pull/removeAllItemsThatMatchASpecifiedPullCondition
     */
    @Test
    @DisplayName("Remove All Items That Match a Specified ``$pull`` Condition")
    public void testRemoveAllItemsThatMatchASpecifiedPullCondition() {
        testUpdate((query) -> query.filter(eq("_id", 1)), pull("votes", gte(6)));
    }

    /**
     * test data: dev/morphia/test/query/updates/pull/removeItemsFromAnArrayOfDocuments
     */
    @Test
    @DisplayName("Remove Items from an Array of Documents")
    public void testRemoveItemsFromAnArrayOfDocuments() {
        testUpdate((query) -> query.filter(), pull("results", eq("score", 8), eq("item", "B")));
    }

}