package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.bitNot;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestBitNot extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/aggregation/expressions/bitNot/bitNotExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testBitNotExpressionOperator() {
        testPipeline(aggregation -> aggregation.pipeline(project().include("result", bitNot("$a"))));
    }
}
