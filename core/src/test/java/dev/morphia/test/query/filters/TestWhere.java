package dev.morphia.test.query.filters;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.where;

public class TestWhere extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/where/example1
     */
    @Test(testName = "main")
    public void testExample1() {
        testQuery(new QueryTestOptions().skipActionCheck(true),
                (query) -> query.filter(
                        where("""
                                function() {
                                   return (hex_md5(this.name) == "9b53e667f30cd329dca1ec9e6a83e994")
                                }""")));
    }
}