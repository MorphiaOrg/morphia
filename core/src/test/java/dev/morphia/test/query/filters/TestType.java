package dev.morphia.test.query.filters;

import dev.morphia.query.Type;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.type;

public class TestType extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/type/example1
     */
    @Test
    @DisplayName("Querying by Data Type")
    public void testExample1() {
        testQuery((query) -> query.filter(type("zipCode", Type.STRING)));
    }

    /**
     * test data: dev/morphia/test/query/filters/type/example2
     */
    @Test
    @DisplayName("Querying by Multiple Data Types")
    public void testExample2() {
        testQuery((query) -> query.filter(type("classAverage", Type.STRING, Type.DOUBLE)));
    }

    /**
     * test data: dev/morphia/test/query/filters/type/example3
     */
    @Test
    @DisplayName("Querying by MinKey and MaxKey")
    public void testExample3() {
        testQuery((query) -> query.filter(type("grades.grade", Type.MIN_KEY)));
    }
}