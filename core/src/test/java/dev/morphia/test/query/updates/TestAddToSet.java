package dev.morphia.test.query.updates;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.addToSet;

public class TestAddToSet extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/addToSet/example1
     * 
     * db.inventory.updateOne( { _id: 1 }, { $addToSet: { tags: "accessories" } } )
     */
    @Test(testName = "Add to Array")
    public void testExample1() {
        testUpdate((query) -> query.filter(eq("_id", 1)), addToSet("tags", "accessories"));
    }

    /**
     * test data: dev/morphia/test/query/updates/addToSet/example2
     * 
     * db.inventory.updateOne( { _id: 1 }, { $addToSet: { tags: "camera" } } )
     */
    @Test(testName = "Value Already Exists")
    public void testExample2() {
        testUpdate((query) -> query.filter(
                eq("_id", 1)),
                addToSet("tags", "camera"));
    }

    /**
     * test data: dev/morphia/test/query/updates/addToSet/example3
     * 
     * db.inventory.updateOne( { _id: 2 }, { $addToSet: { tags: { $each: [ "camera",
     * "electronics", "accessories" ] } } } )
     */
    @Test(testName = "``$each`` Modifier")
    public void testExample3() {
        testUpdate((query) -> query.filter(
                eq("_id", 2)),
                addToSet("tags", List.of("camera", "electronics", "accessories")));
    }
}