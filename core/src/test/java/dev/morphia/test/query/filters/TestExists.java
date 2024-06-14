package dev.morphia.test.query.filters;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.exists;

public class TestExists extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/exists/example1
     */
    @Test(testName = "Exists and Not Equal To")
    public void testExample1() {
        testQuery(new QueryTestOptions().removeIds(true),
                (query) -> query.filter(
                        exists("saffron")));
    }

    /**
     * test data: dev/morphia/test/query/filters/exists/example2
     * 
     * db.spices.find( { saffron: { $exists: true } } )
     */
    @Test(testName = "Null Values")
    public void testExample2() {
        testQuery(new QueryTestOptions().removeIds(true),
                (query) -> query.filter(
                        exists("cinnamon").not()

                ));
    }
}