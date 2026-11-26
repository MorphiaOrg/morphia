package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.SetExpressions.setDifference;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSetDifference extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/setDifference/setDifferenceExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testSetDifferenceExpressionOperator() {
        testPipeline((aggregation) -> aggregation.pipeline(project().suppressId().include("flowerFieldA")
                .include("flowerFieldB").include("inBOnly", setDifference("$flowerFieldB", "$flowerFieldA"))));
    }

}
