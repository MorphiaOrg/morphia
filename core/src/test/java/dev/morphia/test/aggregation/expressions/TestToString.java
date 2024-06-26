package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.StringExpressions;
import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Sort.sort;

public class TestToString extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/toString/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("convertedZipCode", StringExpressions.toString("$zipcode")),
                sort().ascending("convertedZipCode")));
    }

}
