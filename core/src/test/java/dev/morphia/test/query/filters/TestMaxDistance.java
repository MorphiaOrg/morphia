package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestMaxDistance extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/maxDistance/example1
     */
    @Test
    @DisplayName("main")
    public void testExample1() {
        // legacy coordinates just won't be supported for now

        /*
         * testQuery(new QueryTestOptions().skipDataCheck(true), (query) ->
         * query.filter( near("loc", new Point(new Position(-74, 40))),
         * maxDistance("loc",10.0) ));
         */
    }
}