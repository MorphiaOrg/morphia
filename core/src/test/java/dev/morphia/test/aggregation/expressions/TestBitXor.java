package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.bitXor;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestBitXor extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/aggregation/expressions/bitXor/bitXorExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testBitXorExpressionOperator() {
        testPipeline(aggregation -> aggregation.pipeline(project().include("result", bitXor("$a", "$b"))));
    }
}
