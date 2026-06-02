package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.bitsAnyClear;

public class TestBitsAnyClear extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/bitsAnyClear/example1
     * 
     */
    @Test
    @DisplayName("Bit Position Array")
    public void testExample1() {
        testQuery((query) -> query.filter(bitsAnyClear("a", new int[] { 1, 5 })));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAnyClear/example2
     * 
     */
    @Test
    @DisplayName("Integer Bitmask")
    public void testExample2() {
        testQuery((query) -> query.filter(bitsAnyClear("a", 35)));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAnyClear/example3
     * 
     */
    @Test
    @DisplayName("BinData Bitmask")
    public void testExample3() {
        testQuery((query) -> query.filter(bitsAnyClear("a", new byte[] { 48 })));
    }

}
