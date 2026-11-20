package dev.morphia.test.query.filters;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.all;
import static dev.morphia.query.filters.Filters.elemMatch;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gt;

public class TestAll extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/all/useAllToMatchValues
     * 
     */
    @Test
    @DisplayName("Use ``$all`` to Match Values")
    public void testUseAllToMatchValues() {
        testQuery((query) -> query.filter(all("tags", List.of("appliance", "school", "book"))));
    }

    /**
     * test data: dev/morphia/test/query/filters/all/useAllWithElemMatch
     * 
     */
    @Test
    @DisplayName("Use ``$all`` with ``$elemMatch``")
    public void testUseAllWithElemMatch() {
        testQuery((query) -> query.filter(all("qty",
                List.of(elemMatch(eq("size", "M"), gt("num", 50)), elemMatch(eq("num", 100), eq("color", "green"))))));
    }

}
