package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.slice;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSlice extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/slice/sliceExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testSliceExpressionOperator() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("name").include("threeFavorites", slice("$favorites", 3))));
    }

}
