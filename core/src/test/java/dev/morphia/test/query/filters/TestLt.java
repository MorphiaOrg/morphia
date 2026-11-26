package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.lt;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestLt extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/lt/matchDocumentFields
     */
    @Test
    @DisplayName("Match Document Fields")
    public void testMatchDocumentFields() {
        testQuery((query) -> query.filter(lt("quantity", 20)));
    }

    /**
     * test data: dev/morphia/test/query/filters/lt/performAnUpdateBasedOnEmbeddedDocumentFields
     */
    @Test
    @DisplayName("Perform an Update Based on Embedded Document Fields")
    public void testPerformAnUpdateBasedOnEmbeddedDocumentFields() {
        testUpdate((query) -> query.filter(lt("carrier.fee", 20)),
                set("price", 9.99));
    }
}