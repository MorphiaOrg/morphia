package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.substrBytes;
import static dev.morphia.aggregation.expressions.TypeExpressions.toDouble;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestToDouble extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/toDouble/toDoubleExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testToDoubleExpressionOperator() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("degrees", toDouble(substrBytes("$temp", 0, 4)))));
    }

}
