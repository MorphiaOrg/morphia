package dev.morphia.test.query.updates;

import dev.morphia.query.updates.CurrentDateOperator.TypeSpecification;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.currentDate;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestCurrentDate extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/currentDate/example1
     */
    @Test
    @DisplayName("main")
    public void testExample1() {
        testUpdate(new ActionTestOptions().skipDataCheck(true), (query) -> query.filter(eq("_id", 1)),
                currentDate("lastModified"), currentDate("cancellation.date").type(TypeSpecification.TIMESTAMP),
                set("cancellation.reason", "user request"), set("status", "D"));
    }

    /**
     * test data: dev/morphia/test/query/updates/currentDate/example2
     */
    @Test
    @DisplayName("Aggregation Alternative to ``$currentDate``")
    public void testExample2() {
        // this one isn't an operator test as such and updates with a pipeline are
        // tested elsewhere
    }
}