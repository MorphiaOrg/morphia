package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.elemMatch;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.lt;

public class TestElemMatch extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/elemMatch/example1
     * 
     */
    @Test
    @DisplayName("Element Match")
    public void testExample1() {
        testQuery((query) -> query.filter(elemMatch("results", gte(80), lt(85))));
    }

    /**
     * test data: dev/morphia/test/query/filters/elemMatch/example2
     * 
     */
    @Test
    @DisplayName("Array of Embedded Documents")
    public void testExample2() {
        testQuery((query) -> query.filter(elemMatch("results", eq("product", "xyz"), gte("score", 8))));
    }

    /**
     * test data: dev/morphia/test/query/filters/elemMatch/example3
     * 
     */
    @Test
    @DisplayName("Single Query Condition")
    public void testExample3() {
        testQuery((query) -> query.filter(elemMatch("results", eq("product", "xyz"))));
    }

}
