package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestGte extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/gte/matchDocumentFields
     */
    @Test
    @DisplayName("Match Document Fields")
    public void testMatchDocumentFields() {
        testQuery((query) -> query.filter(gte("quantity", 20)));
    }

    /**
     * test data: dev/morphia/test/query/filters/gte/performAnUpdateBasedOnEmbeddedDocumentFields
     */
    @Test
    @DisplayName("Perform an Update Based on Embedded Document Fields")
    public void testPerformAnUpdateBasedOnEmbeddedDocumentFields() {
        testUpdate((query) -> query.filter(gte("carrier.fee", 2)),
                set("price", 9.99));
    }
}