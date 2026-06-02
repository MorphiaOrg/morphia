package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.bitsAllSet;

public class TestBitsAllSet extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/bitsAllSet/example1
     * 
     */
    @Test
    @DisplayName("Bit Position Array")
    public void testExample1() {
        testQuery((query) -> query.filter(bitsAllSet("a", new int[] { 1, 5 })));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAllSet/example2
     * 
     */
    @Test
    @DisplayName("Integer Bitmask")
    public void testExample2() {
        testQuery((query) -> query.filter(bitsAllSet("a", 50)));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAllSet/example3
     * 
     */
    @Test
    @DisplayName("BinData Bitmask")
    public void testExample3() {
        testQuery((query) -> query.filter(bitsAllSet("a", new byte[] { 48 })));
    }

}
