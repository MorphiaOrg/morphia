package dev.morphia.test.query.updates;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.addToSet;

public class TestAddToSet extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/addToSet/addToArray
     */
    @Test
    @DisplayName("Add to Array")
    public void testAddToArray() {
        testUpdate((query) -> query.filter(eq("_id", 1)), addToSet("tags", "accessories"));
    }

    /**
     * test data: dev/morphia/test/query/updates/addToSet/valueAlreadyExists
     */
    @Test
    @DisplayName("Value Already Exists")
    public void testValueAlreadyExists() {
        testUpdate((query) -> query.filter(eq("_id", 1)), addToSet("tags", "camera"));
    }

    /**
     * test data: dev/morphia/test/query/updates/addToSet/eachModifier
     */
    @Test
    @DisplayName("``$each`` Modifier")
    public void testEachModifier() {
        testUpdate((query) -> query.filter(eq("_id", 2)),
                addToSet("tags", List.of("camera", "electronics", "accessories")));
    }
}