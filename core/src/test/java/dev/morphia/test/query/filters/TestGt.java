package dev.morphia.test.query.filters;

import dev.morphia.query.updates.UpdateOperators;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.gt;

public class TestGt extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/gt/matchDocumentFields
     */
    @Test
    @DisplayName("Match Document Fields")
    public void testMatchDocumentFields() {
        testQuery(new ActionTestOptions().removeIds(true), (query) -> query.filter(gt("quantity", 20)));
    }

    /**
     * test data: dev/morphia/test/query/filters/gt/performAnUpdateBasedOnEmbeddedDocumentFields
     */
    @Test
    @DisplayName("Perform an Update Based on Embedded Document Fields")
    public void testPerformAnUpdateBasedOnEmbeddedDocumentFields() {
        testUpdate(new ActionTestOptions().removeIds(true), (query) -> query.filter(gt("carrier.fee", 2)),
                UpdateOperators.set("price", 9.99));
    }
}