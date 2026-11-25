package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.toDate;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Sort.sort;

public class TestToDate extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/toDate/toDateExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testToDateExpressionOperator() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("convertedDate", toDate("$order_date")),
                sort().ascending("convertedDate")));
    }

}
