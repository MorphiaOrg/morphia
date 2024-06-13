package dev.morphia.test.query.filters;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.bitsAnyClear;

public class TestBitsAnyClear extends FilterTest {
    @Test(testName = "Bit Position Array")
    public void testExample1() {
        testQuery((query) -> query.filter(bitsAnyClear("a", new int[] { 1, 5 })));
    }

    @Test(testName = "Integer Bitmask")
    public void testExample2() {
        testQuery((query) -> query.filter(bitsAnyClear("a", 35)));
    }

    @Test(testName = "BinData Bitmask")
    public void testExample3() {
        testQuery((query) -> query.filter(bitsAnyClear("a", new byte[] { 48 })));
    }

}
