package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.log10;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestLog10 extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/log10/log10ExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testLog10ExpressionOperator() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("pH", multiply(-1, log10("$H3O")))));
    }

}
