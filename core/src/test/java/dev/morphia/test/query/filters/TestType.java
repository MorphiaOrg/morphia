package dev.morphia.test.query.filters;

import dev.morphia.query.Type;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.type;

public class TestType extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/type/queryingByDataType
     */
    @Test
    @DisplayName("Querying by Data Type")
    public void testQueryingByDataType() {
        testQuery((query) -> query.filter(type("zipCode", Type.STRING)));
    }

    /**
     * test data: dev/morphia/test/query/filters/type/queryingByMultipleDataTypes
     */
    @Test
    @DisplayName("Querying by Multiple Data Types")
    public void testQueryingByMultipleDataTypes() {
        testQuery((query) -> query.filter(type("classAverage", Type.STRING, Type.DOUBLE)));
    }

    /**
     * test data: dev/morphia/test/query/filters/type/queryingByMinKeyAndMaxKey
     */
    @Test
    @DisplayName("Querying by MinKey and MaxKey")
    public void testQueryingByMinKeyAndMaxKey() {
        testQuery((query) -> query.filter(type("grades.grade", Type.MIN_KEY)));
    }
}