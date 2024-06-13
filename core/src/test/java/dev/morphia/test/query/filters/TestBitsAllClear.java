package dev.morphia.test.query.filters;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.bitsAllClear;

public class TestBitsAllClear extends FilterTest {
    @Test(testName = "Bit Position Array")
    public void testExample1() {
        testQuery((query) -> query.filter(
                bitsAllClear("a", new int[] { 1, 5 })));
    }

    @Test(testName = "Integer Bitmask")
    public void testExample2() {
        testQuery((query) -> query.filter(
                bitsAllClear("a", 35)));
    }

    @Test(testName = "BinData Bitmask")
    public void testExample3() {
        testQuery((query) -> query.filter(
                bitsAllClear("a", new byte[] { 32 })));
    }

}
