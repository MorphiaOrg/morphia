package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.bitsAllSet;

public class TestBitsAllSet extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/bitsAllSet/bitPositionArray
     * 
     */
    @Test
    @DisplayName("Bit Position Array")
    public void testBitPositionArray() {
        testQuery((query) -> query.filter(bitsAllSet("a", new int[] { 1, 5 })));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAllSet/integerBitmask
     * 
     */
    @Test
    @DisplayName("Integer Bitmask")
    public void testIntegerBitmask() {
        testQuery((query) -> query.filter(bitsAllSet("a", 50)));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAllSet/binDataBitmask
     * 
     */
    @Test
    @DisplayName("BinData Bitmask")
    public void testBinDataBitmask() {
        testQuery((query) -> query.filter(bitsAllSet("a", new byte[] { 48 })));
    }

}
