package dev.morphia.test.query.updates;

import java.time.LocalDate;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.min;

public class TestMin extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/min/useMinToCompareNumbers
     */
    @Test
    @DisplayName("Use ``$min`` to Compare Numbers")
    public void testUseMinToCompareNumbers() {
        testUpdate((query) -> query.filter(eq("_id", 1)), min("lowScore", 150));
    }

    /**
     * test data: dev/morphia/test/query/updates/min/useMinToCompareDates
     */
    @Test
    @DisplayName("Use ``$min`` to Compare Dates")
    public void testUseMinToCompareDates() {
        testUpdate((query) -> query.filter(eq("_id", 1)), min("dateEntered", LocalDate.of(2013, 9, 25)));
    }
}