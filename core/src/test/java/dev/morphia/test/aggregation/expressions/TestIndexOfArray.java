package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.indexOfArray;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestIndexOfArray extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/indexOfArray/indexOfArrayExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testIndexOfArrayExpressionOperator() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("index", indexOfArray("$items", 2))));
    }

}
