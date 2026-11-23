package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;

public class TestGroup extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/group/pivotData
     * 
     */
    @Test
    @DisplayName("Pivot Data")
    public void testPivotData() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(group(id("$author")).field("books", push("$title"))));
    }

}
