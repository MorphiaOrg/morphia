package dev.morphia.test.query.updates;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.addToSet;
import static dev.morphia.query.updates.UpdateOperators.push;

public class TestEach extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/each/example1
     */
    @Test(testName = "Use ``$each``  with ``$push`` Operator")
    public void testExample1() {
        testUpdate((query) -> query.filter(
                eq("_id", 1)),
                push("scores", List.of(90, 92, 85)));
    }

    /**
     * test data: dev/morphia/test/query/updates/each/example2
     * 
     * db.inventory.updateOne( { _id: 2 }, { $addToSet: { tags: { $each: [ "camera",
     * "electronics", "accessories" ] } } } )
     */
    @Test(testName = "Use ``$each``  with ``$addToSet`` Operator")
    public void testExample2() {
        testUpdate((query) -> query.filter(
                eq("_id", 2)),
                addToSet("tags", List.of("electronics", "supplies", "camera", "accessories")));
    }
}