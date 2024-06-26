package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.toDate;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Sort.sort;

public class TestToDate extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/toDate/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("convertedDate", toDate("$order_date")),
                sort().ascending("convertedDate")));
    }

}
