package dev.morphia.test.query.filters;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.mod;

public class TestMod extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/mod/example1
     * 
     * db.inventory.find( { qty: { $mod: [ 4, 0 ] } } )
     */
    @Test(testName = "Use ``$mod`` to Select Documents")
    public void testExample1() {
        testQuery((query) -> query.filter(
                mod("qty", 4, 0)));
    }

    /**
     * test data: dev/morphia/test/query/filters/mod/example2
     * 
     * db.inventory.find( { qty: { $mod: [ 4 ] } } )
     */
    @Test(testName = "Not Enough Elements Error")
    public void testExample2() {
        // ignored
    }

    /**
     * test data: dev/morphia/test/query/filters/mod/example3
     */
    @Test(testName = "Too Many Elements Error")
    public void testExample3() {
        // ignored
    }

    /**
     * test data: dev/morphia/test/query/filters/mod/example4
     */
    @Test(testName = "Floating Point Arguments")
    public void testExample4() {
        testQuery((query) -> query.filter(
                mod("qty", 4.0, 0.0)

        ));
    }

    /**
     * test data: dev/morphia/test/query/filters/mod/example5
     */
    @Test(testName = "Negative Dividend")
    public void testExample5() {
        testQuery((query) -> query.filter(
                mod("qty", -4, -0)

        ));
    }
}