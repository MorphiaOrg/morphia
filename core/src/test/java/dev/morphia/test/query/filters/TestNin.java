package dev.morphia.test.query.filters;

import java.util.List;

import dev.morphia.query.FindOptions;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.nin;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestNin extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/nin/selectOnUnmatchingDocuments
     */
    @Test
    @DisplayName("Select on Unmatching Documents")
    public void testSelectOnUnmatchingDocuments() {
        testQuery((query) -> query.filter(nin("quantity", List.of(5, 15))), new FindOptions().projection().exclude("_id"));
    }

    /**
     * test data: dev/morphia/test/query/filters/nin/selectOnElementsNotInAnArray
     */
    @Test
    @DisplayName("Select on Elements Not in an Array")
    public void testSelectOnElementsNotInAnArray() {
        testUpdate((query) -> query.filter(nin("genres", List.of("Drama"))), set("exclude", true));
    }
}
