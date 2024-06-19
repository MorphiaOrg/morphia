package dev.morphia.test.query.filters;

import java.util.List;

import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.nin;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestNin extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/nin/example1
     */
    @Test(testName = "Select on Unmatching Documents")
    public void testExample1() {
        testQuery(new ActionTestOptions().removeIds(true), (query) -> query.filter(nin("quantity", List.of(5, 15))));
    }

    /**
     * test data: dev/morphia/test/query/filters/nin/example2
     * 
     * db.inventory.updateMany( { tags: { $nin: [ "school" ] } }, { $set: { exclude:
     * true } } )
     */
    @Test(testName = "Select on Elements Not in an Array")
    public void testExample2() {
        testUpdate(new ActionTestOptions().removeIds(true), (query) -> query.filter(nin("tags", List.of("school"))),
                set("exclude", true));
    }
}