package dev.morphia.test.query.filters;

import dev.morphia.test.ServerVersion;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.bitsAnySet;

public class TestBitsAnySet extends FilterTest {
    @Test(testName = "Bit Position Array")
    public void testExample1() {
        testQuery(ServerVersion.ANY, false, true, (query) -> query.filter(
                bitsAnySet("a", new int[] { 1, 5 })));
    }

    @Test(testName = "Integer Bitmask")
    public void testExample2() {
        testQuery(ServerVersion.ANY, false, true, (query) -> query.filter(
                bitsAnySet("a", 35)));
    }

    @Test(testName = "BinData Bitmask")
    public void testExample3() {
        testQuery(ServerVersion.ANY, false, true, (query) -> query.filter(
                bitsAnySet("a", new byte[] { 48 })));
    }

}
