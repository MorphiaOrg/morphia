package dev.morphia.test.query.filters;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.all;
import static dev.morphia.query.filters.Filters.elemMatch;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gt;

public class TestAll extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/all/example1
     * 
     */
    @Test(testName = "Use ``$all`` to Match Values")
    public void testExample1() {
        testQuery((query) -> query.filter(all("tags", List.of("appliance", "school", "book"))));
    }

    /**
     * test data: dev/morphia/test/query/filters/all/example2
     * 
     */
    @Test(testName = "Use ``$all`` with ``$elemMatch``")
    public void testExample2() {
        testQuery((query) -> query.filter(all("qty",
                List.of(elemMatch(eq("size", "M"), gt("num", 50)), elemMatch(eq("num", 100), eq("color", "green"))))));
    }

}
