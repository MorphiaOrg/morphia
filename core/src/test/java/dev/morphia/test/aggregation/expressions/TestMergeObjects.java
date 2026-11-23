package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ObjectExpressions.mergeObjects;
import static dev.morphia.aggregation.expressions.SystemVariables.*;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;

public class TestMergeObjects extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/mergeObjects/mergeObjectsAsAnAccumulator
     * 
     */
    @Test
    @DisplayName("``$mergeObjects`` as an Accumulator")
    public void testMergeObjectsAsAnAccumulator() {
        testPipeline(new ActionTestOptions().orderMatters(false), (aggregation) -> aggregation
                .pipeline(group(id("$item")).field("mergedSales", mergeObjects().add("$quantity"))));
    }

}
