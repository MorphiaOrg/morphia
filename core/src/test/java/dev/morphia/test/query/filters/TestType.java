package dev.morphia.test.query.filters;

import dev.morphia.query.FindOptions;
import dev.morphia.query.Type;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.type;

public class TestType extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/type/queryingByMultipleDataTypes
     */
    @Test
    @DisplayName("Querying by Multiple Data Types")
    public void testQueryingByMultipleDataTypes() {
        testQuery((query) -> query.filter(type("classAverage", Type.STRING, Type.DOUBLE)));
    }

    @Test
    @DisplayName("Match Several Types by Alias")
    public void testMatchSeveralTypesByAlias() {
        testQuery((query) -> query.filter(type("imdb.rating", Type.STRING, Type.DOUBLE), gt("runtime", 1000)),
                new FindOptions().projection().include("title", "runtime", "imdb.rating").projection().exclude("_id"));
    }

    @Test
    @DisplayName("Match String Ratings by Type Alias")
    public void testMatchStringRatingsByTypeAlias() {
        testQuery((query) -> query.filter(type("imdb.rating", Type.STRING), eq("year", 2013)),
                new FindOptions().projection().include("title", "year", "imdb.rating").projection().exclude("_id"));
    }

    @Test
    @DisplayName("Querying by Array Type")
    public void testQueryingByArrayType() {
        testQuery((query) -> query.filter(type("genres", Type.ARRAY), gt("runtime", 1000)),
                new FindOptions().projection().include("title", "genres").projection().exclude("_id"));
    }
}
