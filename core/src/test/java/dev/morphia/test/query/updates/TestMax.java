package dev.morphia.test.query.updates;

import java.time.LocalDate;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.max;

public class TestMax extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/max/useMaxToCompareNumbers
     */
    @Test
    @DisplayName("Use ``$max`` to Compare Numbers")
    public void testUseMaxToCompareNumbers() {
        testUpdate((query) -> query.filter(eq("_id", 1)), max("highScore", 950));
    }

    /**
     * test data: dev/morphia/test/query/updates/max/useMaxToCompareDates
     */
    @Test
    @DisplayName("Use ``$max`` to Compare Dates")
    public void testUseMaxToCompareDates() {
        testUpdate((query) -> query.filter(eq("_id", 1)), max("dateExpired", LocalDate.of(2013, 9, 30)));
    }
}