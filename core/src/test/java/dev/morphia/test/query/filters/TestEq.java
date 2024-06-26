package dev.morphia.test.query.filters;

import java.util.List;
import java.util.regex.Pattern;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;

public class TestEq extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/eq/example1
     * 
     */
    @Test(testName = "Equals a Specified Value")
    public void testExample1() {
        testQuery((query) -> query.filter(eq("qty", 20)));
    }

    /**
     * test data: dev/morphia/test/query/filters/eq/example2
     * 
     */
    @Test(testName = "Field in Embedded Document Equals a Value")
    public void testExample2() {
        testQuery((query) -> query.filter(eq("item.name", "ab")));
    }

    /**
     * test data: dev/morphia/test/query/filters/eq/example3
     * 
     */
    @Test(testName = "Array Element Equals a Value")
    public void testExample3() {
        testQuery((query) -> query.filter(eq("tags", "B")));
    }

    /**
     * test data: dev/morphia/test/query/filters/eq/example4
     * 
     */
    @Test(testName = "Equals an Array Value")
    public void testExample4() {
        testQuery((query) -> query.filter(eq("tags", List.of("A", "B"))

        ));
    }

    /**
     * test data: dev/morphia/test/query/filters/eq/example5
     * 
     */
    @Test(testName = "Regex Match Behaviour")
    public void testExample5() {
        testQuery(new ActionTestOptions().removeIds(true),
                (query) -> query.filter(eq("company", Pattern.compile("MongoDB"))));
    }

}
