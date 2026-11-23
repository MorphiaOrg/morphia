package dev.morphia.test.query.filters;

import java.util.List;

import dev.morphia.query.FindOptions;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.nin;

public class TestNin extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/nin/selectOnUnmatchingDocuments
     */
    @Test
    @DisplayName("Select on Unmatching Documents")
    public void testSelectOnUnmatchingDocuments() {
        testQuery(new ActionTestOptions().removeIds(true).findOptions(new FindOptions().projection().exclude("_id")),
                (query) -> query.filter(nin("quantity", List.of(5, 15))));
    }

}