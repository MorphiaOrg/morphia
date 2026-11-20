package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.bitsAllClear;

public class TestBitsAllClear extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/bitsAllClear/bitPositionArray
     * 
     */
    @Test
    @DisplayName("Bit Position Array")
    public void testBitPositionArray() {
        testQuery((query) -> query.filter(bitsAllClear("a", new int[] { 1, 5 })));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAllClear/integerBitmask
     * 
     */
    @Test
    @DisplayName("Integer Bitmask")
    public void testIntegerBitmask() {
        testQuery((query) -> query.filter(bitsAllClear("a", 35)));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAllClear/binDataBitmask
     * 
     */
    @Test
    @DisplayName("BinData Bitmask")
    public void testBinDataBitmask() {
        testQuery((query) -> query.filter(bitsAllClear("a", new byte[] { 32 })));
    }

}
