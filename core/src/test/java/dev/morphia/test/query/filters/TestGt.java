package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestGt extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/gt/matchDocumentFields
     */
    @Test
    @DisplayName("Match Document Fields")
    public void testMatchDocumentFields() {
        testQuery((query) -> query.filter(gt("quantity", 20)));
    }

    @Test
    @DisplayName("Perform an Update Based on Embedded Document Fields")
    public void testPerformAnUpdateBasedOnEmbeddedDocumentFields() {
        testUpdate((query) -> query.filter(gt("imdb.rating", 9.5)),
                set("highestRated", true));
    }
}
