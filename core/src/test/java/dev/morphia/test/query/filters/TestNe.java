package dev.morphia.test.query.filters;

import java.util.List;

import dev.morphia.query.FindOptions;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.ne;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestNe extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/ne/matchDocumentFieldsThatAreNotEqual
     */
    @Test
    @DisplayName("Match Document Fields That Are Not Equal")
    public void testMatchDocumentFieldsThatAreNotEqual() {
        testQuery((query) -> query.filter(ne("quantity", 20)));
    }

    @Test
    @DisplayName("Use a Scalar Comparison")
    public void testUseAScalarComparison() {
        testQuery((query) -> query.filter(ne("genres", "Drama"), gt("runtime", 1000)),
                new FindOptions().projection().include("title", "genres").projection().exclude("_id"));
    }

    @Test
    @DisplayName("Use an Exact Array Comparison")
    public void testUseAnExactArrayComparison() {
        testQuery((query) -> query.filter(ne("genres", List.of("Drama")), gt("runtime", 1000)),
                new FindOptions().projection().include("title", "genres").projection().exclude("_id"));
    }

    /**
     * test data: dev/morphia/test/query/filters/ne/updateBasedOnNotEqualEmbeddedDocumentFields
     */
    @Test
    @DisplayName("Update Based on Not Equal Embedded Document Fields")
    public void testUpdateBasedOnNotEqualEmbeddedDocumentFields() {
        testUpdate((query) -> query.filter(ne("imdb.rating", 9.3)), set("highestRated", false));
    }
}
