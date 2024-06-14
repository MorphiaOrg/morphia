package dev.morphia.test.query.filters;

import java.util.List;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.nin;

public class TestNin extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/nin/example1
     * 
     * db.inventory.find( { quantity: { $nin: [ 5, 15 ] } }, { _id: 0 } )
     */
    @Test(testName = "Select on Unmatching Documents")
    public void testExample1() {
        testQuery(new QueryTestOptions().removeIds(true),
                (query) -> query.filter(
                        nin("quantity", List.of(5, 15))));
    }
}