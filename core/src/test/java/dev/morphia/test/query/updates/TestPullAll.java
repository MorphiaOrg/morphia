package dev.morphia.test.query.updates;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.pullAll;

public class TestPullAll extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/pullAll/example1
     */
    @Test(testName = "main")
    public void testExample1() {
        testUpdate((query) -> query.filter(
                eq("_id", 1)),
                pullAll("scores", List.of(0, 5)));
    }
}