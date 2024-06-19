package dev.morphia.test.query.filters;

import org.testng.annotations.Test;

public class TestMinDistance extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/minDistance/example1
     */
    @Test(testName = "Use with ``$near``")
    public void testExample1() {
        // already tested elsewhere
    }

    /**
     * test data: dev/morphia/test/query/filters/minDistance/example2
     */
    @Test(testName = "Use with ``$nearSphere``")
    public void testExample2() {
        // legacy coordinates just won't be supported for now
    }
}