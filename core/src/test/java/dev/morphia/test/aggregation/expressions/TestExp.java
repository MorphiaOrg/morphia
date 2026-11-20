package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.exp;
import static dev.morphia.aggregation.expressions.MathExpressions.subtract;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestExp extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/exp/expExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testExpExpressionOperator() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("effectiveRate", subtract(exp("$interestRate"), 1))));
    }

}
