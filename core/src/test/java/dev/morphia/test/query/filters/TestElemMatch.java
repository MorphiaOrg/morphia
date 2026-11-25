package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.elemMatch;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.lt;
import static dev.morphia.query.filters.Filters.ne;

public class TestElemMatch extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/elemMatch/elementMatch
     * 
     */
    @Test
    @DisplayName("Element Match")
    public void testElementMatch() {
        testQuery((query) -> query.filter(elemMatch("results", gte(80), lt(85))));
    }

    /**
     * test data: dev/morphia/test/query/filters/elemMatch/arrayOfEmbeddedDocuments
     * 
     */
    @Test
    @DisplayName("Array of Embedded Documents")
    public void testArrayOfEmbeddedDocuments() {
        testQuery((query) -> query.filter(elemMatch("results", eq("product", "xyz"), gte("score", 8))));
    }

    @Test
    @DisplayName("Match an Element with Dot Notation")
    public void testMatchAnElementWithDotNotation() {
        testQuery((query) -> query.filter(eq("results.product", "xyz")));
    }

    @Test
    @DisplayName("Match When No Element Is xyz")
    public void testMatchWhenNoElementIsXyz() {
        testQuery((query) -> query.filter(ne("results.product", "xyz")));
    }
}
