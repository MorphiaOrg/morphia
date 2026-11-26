package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.stages.Unset.unset;

public class TestUnset extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/unset/removeASingleField
     * 
     */
    @Test
    @DisplayName("Remove a Single Field")
    public void testRemoveASingleField() {
        testPipeline((aggregation) -> aggregation.pipeline(unset("copies")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/unset/removeTopLevelFields
     * 
     */
    @Test
    @DisplayName("Remove Top-Level Fields")
    public void testRemoveTopLevelFields() {
        testPipeline((aggregation) -> aggregation.pipeline(unset("isbn", "copies")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/unset/removeEmbeddedFields
     * 
     */
    @Test
    @DisplayName("Remove Embedded Fields")
    public void testRemoveEmbeddedFields() {
        testPipeline((aggregation) -> aggregation.pipeline(unset("isbn", "author.first", "copies.warehouse")));
    }
}
