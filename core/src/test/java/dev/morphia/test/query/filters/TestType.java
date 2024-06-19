package dev.morphia.test.query.filters;

import dev.morphia.query.Type;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.type;

public class TestType extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/type/example1
     */
    @Test(testName = "Querying by Data Type")
    public void testExample1() {
        testQuery((query) -> query.filter(type("zipCode", Type.STRING)));
    }

    /**
     * test data: dev/morphia/test/query/filters/type/example2
     */
    @Test(testName = "Querying by Multiple Data Types")
    public void testExample2() {
        testQuery((query) -> query.filter(type("classAverage", Type.STRING, Type.DOUBLE)));
    }

    /**
     * test data: dev/morphia/test/query/filters/type/example3
     */
    @Test(testName = "Querying by MinKey and MaxKey")
    public void testExample3() {
        testQuery((query) -> query.filter(type("grades.grade", Type.MIN_KEY)));
    }
}