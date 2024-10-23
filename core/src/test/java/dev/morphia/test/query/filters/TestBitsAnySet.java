package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.bitsAnySet;

public class TestBitsAnySet extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/bitsAnySet/example1
     * 
     */
    @Test(testName = "Bit Position Array")
    public void testExample1() {
        testQuery((query) -> query.filter(bitsAnySet("a", new int[] { 1, 5 })));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAnySet/example2
     * 
     */
    @Test(testName = "Integer Bitmask")
    public void testExample2() {
        testQuery((query) -> query.filter(bitsAnySet("a", 35)));
    }

    /**
     * test data: dev/morphia/test/query/filters/bitsAnySet/example3
     * 
     */
    @Test(testName = "BinData Bitmask")
    public void testExample3() {
        testQuery((query) -> query.filter(bitsAnySet("a", new byte[] { 48 })));
    }

}
