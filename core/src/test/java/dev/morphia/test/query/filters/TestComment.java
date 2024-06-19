package dev.morphia.test.query.filters;

import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.comment;
import static dev.morphia.query.filters.Filters.mod;

public class TestComment extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/comment/example1
     */
    @Test(testName = "Attach a Comment to ``find``")
    public void testExample1() {
        ActionTestOptions options = new ActionTestOptions().skipDataCheck(true);
        testQuery(options, (query) -> query.filter(mod("x", 2, 0), comment("Find even values.")));
    }

    /**
     * test data: dev/morphia/test/query/filters/comment/example2
     */
    @Test(testName = "Attach a Comment to an Aggregation Expression")
    public void testExample2() {
        // this example is for an aggregation
    }
}