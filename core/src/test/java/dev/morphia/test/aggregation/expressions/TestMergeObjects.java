package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ObjectExpressions.mergeObjects;
import static dev.morphia.aggregation.expressions.SystemVariables.*;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Sort.sort;

public class TestMergeObjects extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/mergeObjects/mergeObjectsAsAnAccumulator
     * 
     */
    @Test
    @DisplayName("``$mergeObjects`` as an Accumulator")
    public void testMergeObjectsAsAnAccumulator() {
        testPipeline((aggregation) -> aggregation
                .pipeline(group(id("$item")).field("mergedSales", mergeObjects().add("$quantity"))));
    }

    @Test
    @DisplayName("Merge the Ratings of Each Movie")
    public void testMergeTheRatingsOfEachMovie() {
        testPipeline((aggregation) -> aggregation.pipeline(
                group(id("$title")).field("allRatings", mergeObjects().add("$data")),
                sort().ascending("_id")));
    }
}
