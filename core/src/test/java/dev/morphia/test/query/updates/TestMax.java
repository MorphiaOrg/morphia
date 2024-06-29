package dev.morphia.test.query.updates;

import java.time.LocalDate;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.max;

public class TestMax extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/max/example1
     */
    @Test(testName = "Use ``$max`` to Compare Numbers")
    public void testExample1() {
        testUpdate((query) -> query.filter(eq("_id", 1)), max("highScore", 950));
    }

    /**
     * test data: dev/morphia/test/query/updates/max/example2
     */
    @Test(testName = "Use ``$max`` to Compare Dates")
    public void testExample2() {
        testUpdate((query) -> query.filter(eq("_id", 1)), max("dateExpired", LocalDate.of(2013, 9, 30)));
    }
}