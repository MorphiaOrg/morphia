package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.exists;

public class TestExists extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/exists/example1
     */
    @Test
    @DisplayName("Exists and Not Equal To")
    public void testExample1() {
        testQuery(new ActionTestOptions().removeIds(true), (query) -> query.filter(exists("saffron")));
    }

    /**
     * test data: dev/morphia/test/query/filters/exists/example2
     */
    @Test
    @DisplayName("Null Values")
    public void testExample2() {
        testQuery(new ActionTestOptions().removeIds(true), (query) -> query.filter(exists("cinnamon").not()

        ));
    }
}