package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.DataSizeExpressions.binarySize;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestBinarySize extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/binarySize/binarySizeExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testBinarySizeExpressionOperator() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("name", "$name").include("imageSize", binarySize("$binary"))));
    }

}
