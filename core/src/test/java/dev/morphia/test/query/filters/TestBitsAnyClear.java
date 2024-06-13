package dev.morphia.test.query.filters;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.bitsAnyClear;

public class TestBitsAnyClear extends FilterTest {
    /**
     * test data: dev/morphia/test/query/filters/bitsAnyClear/example1
     * 
     */
    @Test(testName = "Bit Position Array")
    public void testExample1() {
        testQuery((query) -> query.filter(bitsAnyClear("a", new int[] { 1, 5 })));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAnyClear/example2
     * 
     */
    @Test(testName = "Integer Bitmask")
    public void testExample2() {
        testQuery((query) -> query.filter(bitsAnyClear("a", 35)));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAnyClear/example3
     * 
     */
    @Test(testName = "BinData Bitmask")
    public void testExample3() {
        testQuery((query) -> query.filter(bitsAnyClear("a", new byte[] { 48 })));
    }

}
