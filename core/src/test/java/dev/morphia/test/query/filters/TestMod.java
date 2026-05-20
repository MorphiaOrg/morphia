package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.mod;

public class TestMod extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/mod/example1
     */
    @Test
    @DisplayName("Use ``$mod`` to Select Documents")
    public void testExample1() {
        testQuery((query) -> query.filter(mod("qty", 4, 0)));
    }

    /**
     * test data: dev/morphia/test/query/filters/mod/example2
     */
    @Test
    @DisplayName("Not Enough Elements Error")
    public void testExample2() {
        // ignored
    }

    /**
     * test data: dev/morphia/test/query/filters/mod/example3
     */
    @Test
    @DisplayName("Too Many Elements Error")
    public void testExample3() {
        // ignored
    }

    /**
     * test data: dev/morphia/test/query/filters/mod/example4
     */
    @Test
    @DisplayName("Floating Point Arguments")
    public void testExample4() {
        testQuery((query) -> query.filter(mod("qty", 4.0, 0.0)));
    }

    /**
     * test data: dev/morphia/test/query/filters/mod/example5
     */
    @Test
    @DisplayName("Negative Dividend")
    public void testExample5() {
        testQuery((query) -> query.filter(mod("qty", -4, -0)));
    }
}