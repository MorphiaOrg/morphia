package dev.morphia.test.query.filters;

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

}