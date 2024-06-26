package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.shift;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.descending;

public class TestShift extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/shift/example1
     * 
     */
    @Test(testName = "Shift Using a Positive Integer")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(setWindowFields().partitionBy("$state").sortBy(descending("quantity"))
                        .output(output("shiftQuantityForState").operator(shift("$quantity", 1, "Not available")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/shift/example2
     * 
     */
    @Test(testName = "Shift Using a Negative Integer")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation
                .pipeline(setWindowFields().partitionBy("$state").sortBy(descending("quantity"))
                        .output(output("shiftQuantityForState").operator(shift("$quantity", -1, "Not available")))));
    }

}
