package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.bitsAnySet;

public class TestBitsAnySet extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/bitsAnySet/bitPositionArray
     * 
     */
    @Test
    @DisplayName("Bit Position Array")
    public void testBitPositionArray() {
        testQuery((query) -> query.filter(bitsAnySet("a", new int[] { 1, 5 })));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAnySet/integerBitmask
     * 
     */
    @Test
    @DisplayName("Integer Bitmask")
    public void testIntegerBitmask() {
        testQuery((query) -> query.filter(bitsAnySet("a", 35)));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAnySet/binDataBitmask
     * 
     */
    @Test
    @DisplayName("BinData Bitmask")
    public void testBinDataBitmask() {
        testQuery((query) -> query.filter(bitsAnySet("a", new byte[] { 48 })));
    }

}
