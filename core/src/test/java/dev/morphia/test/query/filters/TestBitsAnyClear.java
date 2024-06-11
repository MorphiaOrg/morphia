package dev.morphia.test.query.filters;

import dev.morphia.test.ServerVersion;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.bitsAnyClear;

public class TestBitsAnyClear extends FilterTest {
    @Test
    public void testExample1() {
        testQuery(ServerVersion.ANY, false, true, (query) -> query.filter(
                bitsAnyClear("a", new int[] { 1, 5 })));
    }

    @Test
    public void testExample2() {
        testQuery(ServerVersion.ANY, false, true, (query) -> query.filter(
                bitsAnyClear("a", 35)));
    }

    @Test
    public void testExample3() {
        testQuery(ServerVersion.ANY, false, true, (query) -> query.filter(
                bitsAnyClear("a", new byte[] { 48 })));
    }

}
