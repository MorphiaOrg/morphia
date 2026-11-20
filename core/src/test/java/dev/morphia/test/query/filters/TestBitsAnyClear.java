package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.bitsAnyClear;

public class TestBitsAnyClear extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/bitsAnyClear/bitPositionArray
     * 
     */
    @Test
    @DisplayName("Bit Position Array")
    public void testBitPositionArray() {
        testQuery((query) -> query.filter(bitsAnyClear("a", new int[] { 1, 5 })));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAnyClear/integerBitmask
     * 
     */
    @Test
    @DisplayName("Integer Bitmask")
    public void testIntegerBitmask() {
        testQuery((query) -> query.filter(bitsAnyClear("a", 35)));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAnyClear/binDataBitmask
     * 
     */
    @Test
    @DisplayName("BinData Bitmask")
    public void testBinDataBitmask() {
        testQuery((query) -> query.filter(bitsAnyClear("a", new byte[] { 48 })));
    }

}
