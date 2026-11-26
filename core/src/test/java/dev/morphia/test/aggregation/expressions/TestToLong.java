package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.TypeExpressions.toLong;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Sort.sort;

public class TestToLong extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/toLong/toLongExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testToLongExpressionOperator() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("convertedQty", toLong("$qty")),
                sort().descending("convertedQty")));
    }

}
