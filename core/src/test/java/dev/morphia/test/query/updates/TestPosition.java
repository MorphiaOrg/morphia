package dev.morphia.test.query.updates;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.push;

public class TestPosition extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/position/example1
     */
    @Test(testName = "Add Elements at the Start of the Array")
    public void testExample1() {
        testUpdate((query) -> query.filter(eq("_id", 1)), push("scores", List.of(50, 60, 70)).position(0));
    }

    /**
     * test data: dev/morphia/test/query/updates/position/example2
     */
    @Test(testName = "Add Elements to the Middle of the Array")
    public void testExample2() {
        testUpdate((query) -> query.filter(eq("_id", 2)), push("scores", List.of(20, 30)).position(2));
    }

    /**
     * test data: dev/morphia/test/query/updates/position/example3
     */
    @Test(testName = "Use a Negative Array Index (Position) to Add Elements to the Array")
    public void testExample3() {
        testUpdate((query) -> query.filter(eq("_id", 3)), push("scores", List.of(90, 80)).position(-2));
    }
}