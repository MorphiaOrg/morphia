package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.ne;

public class TestNe extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/ne/matchDocumentFieldsThatAreNotEqual
     */
    @Test
    @DisplayName("Match Document Fields That Are Not Equal")
    public void testMatchDocumentFieldsThatAreNotEqual() {
        testQuery(new ActionTestOptions().removeIds(true), (query) -> query.filter(ne("quantity", 20)));
    }

}