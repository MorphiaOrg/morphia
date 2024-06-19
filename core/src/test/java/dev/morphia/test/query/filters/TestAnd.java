package dev.morphia.test.query.filters;

import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.and;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.exists;
import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.lt;
import static dev.morphia.query.filters.Filters.ne;
import static dev.morphia.query.filters.Filters.or;

public class TestAnd extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/and/example1
     */
    @Test(testName = "``AND`` Queries With Multiple Expressions Specifying the Same Field")
    public void testExample1() {
        testQuery(new ActionTestOptions().skipDataCheck(true),
                (query) -> query.filter(and(ne("price", 1.99), exists("price"))));
    }

    /**
     * test data: dev/morphia/test/query/filters/and/example2
     */
    @Test(testName = "``AND`` Queries With Multiple Expressions Specifying the Same Operator")
    public void testExample2() {
        testQuery(new ActionTestOptions().skipDataCheck(true),
                (query) -> query.filter(and(or(lt("qty", 10), gt("qty", 50)), or(eq("sale", true), lt("price", 5)))));
    }
}