package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.mod;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestMod extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/mod/modExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testModExpressionOperator() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("remainder", mod("$hours", "$tasks"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/mod/negativeDividend
     * 
     */
    @Test
    @DisplayName("Negative Dividend")
    public void testNegativeDividend() {
        testPipeline(
                (aggregation) -> aggregation.pipeline(project().include("remainder", mod("$dividend", "$divisor"))));
    }

}
