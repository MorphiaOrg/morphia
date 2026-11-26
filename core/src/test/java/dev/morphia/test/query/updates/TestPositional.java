package dev.morphia.test.query.updates;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.elemMatch;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.lte;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestPositional extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/positional/updateValuesInAnArray
     */
    @Test
    @DisplayName("Update Values in an Array")
    public void testUpdateValuesInAnArray() {
        testUpdate((query) -> query.filter(eq("_id", 1), eq("grades", 80)),
                set("grades.$", 82));
    }

    /**
     * test data: dev/morphia/test/query/updates/positional/updateEmbeddedDocumentsUsingMultipleFieldMatches
     */
    @Test
    @DisplayName("Update Embedded Documents Using Multiple Field Matches")
    public void testUpdateEmbeddedDocumentsUsingMultipleFieldMatches() {
        testUpdate((query) -> query.filter(eq("_id", 5), elemMatch("grades", lte("grade", 90), gt("mean", 80))),
                set("grades.$.std", 6));
    }

    /**
     * test data: dev/morphia/test/query/updates/positional/updateWithMultipleArrayMatches
     */
    @Test
    @DisplayName("Update with Multiple Array Matches")
    public void testUpdateWithMultipleArrayMatches() {
        testUpdate((query) -> query.filter(eq("activity_ids", 1), eq("grades", 95), eq("deans_list", 2021)),
                set("deans_list.$", 2022));
    }

}