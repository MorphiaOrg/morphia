package dev.morphia.test.query.updates;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.pop;

public class TestPop extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/pop/example1
     */
    @Test
    @DisplayName("Remove the First Item of an Array")
    public void testExample1() {
        testUpdate((query) -> query.filter(eq("_id", 1)), pop("scores").removeFirst());
    }

    /**
     * test data: dev/morphia/test/query/updates/pop/example2
     */
    @Test
    @DisplayName("Remove the Last Item of an Array")
    public void testExample2() {
        testUpdate((query) -> query.filter(eq("_id", 10)), pop("scores"));
    }
}