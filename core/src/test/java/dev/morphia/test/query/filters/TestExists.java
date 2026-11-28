package dev.morphia.test.query.filters;

import java.util.List;

import dev.morphia.query.FindOptions;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.exists;
import static dev.morphia.query.filters.Filters.nin;

public class TestExists extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/exists/existsTrue
     */
    @Test
    @DisplayName("$exists: true")
    public void testExistsTrue() {
        testQuery((query) -> query.filter(exists("rated")),
                new FindOptions().projection().include("title", "rated").projection().exclude("_id").limit(3));
    }

    /**
     * test data: dev/morphia/test/query/filters/exists/existsFalse
     */
    @Test
    @DisplayName("$exists: false")
    public void testExistsFalse() {
        testQuery((query) -> query.filter(exists("rated").not()),
                new FindOptions().projection().include("title", "year").projection().exclude("_id").limit(3));
    }

    /**
     * test data: dev/morphia/test/query/filters/exists/existsAndNotEqualTo
     */
    @Test
    @DisplayName("Exists and Not Equal To")
    public void testExistsAndNotEqualTo() {
        testQuery((query) -> query.filter(exists("rated"), nin("rated", List.of("R", "PG-13"))),
                new FindOptions().limit(5));
    }
}
