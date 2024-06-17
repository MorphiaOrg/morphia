package dev.morphia.test.query.filters;

import org.testng.annotations.Test;

public class TestMaxDistance extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/maxDistance/example1
     */
    @Test(testName = "main")
    public void testExample1() {
        // legacy coordinates just won't be supported for now

        /*
         * testQuery(new QueryTestOptions().skipDataCheck(true),
         * (query) -> query.filter(
         * near("loc", new Point(new Position(-74, 40))),
         * maxDistance("loc",10.0)
         * ));
         */
    }
}