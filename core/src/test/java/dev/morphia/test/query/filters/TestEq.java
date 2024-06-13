package dev.morphia.test.query.filters;

import java.util.List;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;

public class TestEq extends FilterTest {
    @Test(testName = "Equals a Specified Value")
    public void testExample1() {
        testQuery((query) -> query.filter(eq("qty", 20)));
    }

    @Test(testName = "Field in Embedded Document Equals a Value")
    public void testExample2() {
        testQuery((query) -> query.filter(eq("item.name", "ab")));
    }

    @Test(testName = "Array Element Equals a Value")
    public void testExample3() {
        testQuery((query) -> query.filter(eq("tags", "B")));
    }

    @Test(testName = "Equals an Array Value")
    public void testExample4() {
        testQuery((query) -> query.filter(eq("tags", List.of("A", "B"))

        ));
    }

    @Test(testName = "Regex Match Behaviour")
    public void testExample5() {
        testQuery(new QueryTestOptions().removeIds(true),
                (query) -> query.filter(eq("company", Pattern.compile("MongoDB"))));
    }

}
