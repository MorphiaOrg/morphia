package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.trunc;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestTrunc extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/trunc/truncExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testTruncExpressionOperator() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("truncatedValue", trunc("$value", 1))));
    }

    @Test
    @DisplayName("Truncate to a Whole Number")
    public void testTruncateToAWholeNumber() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().include("truncatedValue", trunc("$value", 0))));
    }

    @Test
    @DisplayName("Truncate to Tens")
    public void testTruncateToTens() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().include("truncatedValue", trunc("$value", -1))));
    }
}
