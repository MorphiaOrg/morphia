package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.exists;
import static dev.morphia.query.filters.Filters.lt;
import static dev.morphia.query.filters.Filters.nor;

public class TestNor extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/nor/norQueryWithTwoExpressions
     */
    @Test
    @DisplayName("``$nor`` Query with Two Expressions")
    public void testNorQueryWithTwoExpressions() {
        testQuery((query) -> query.filter(nor(eq("price", 1.99), eq("sale", true))));
    }

    /**
     * test data: dev/morphia/test/query/filters/nor/norAndAdditionalComparisons
     */
    @Test
    @DisplayName("``$nor`` and Additional Comparisons")
    public void testNorAndAdditionalComparisons() {
        testQuery((query) -> query.filter(nor(eq("price", 1.99), lt("qty", 20), eq("sale", true))));
    }

    /**
     * test data: dev/morphia/test/query/filters/nor/norAndExists
     */
    @Test
    @DisplayName("``$nor`` and ``$exists``")
    public void testNorAndExists() {
        testQuery((query) -> query
                .filter(nor(eq("price", 1.99), exists("price").not(), eq("sale", true), exists("sale").not())

                ));
    }
}