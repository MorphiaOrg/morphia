package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.linearFill;
import static dev.morphia.aggregation.expressions.WindowExpressions.locf;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.test.ServerVersion.v52;

public class TestLocf extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/locf/example1
     * 
     */
    @Test(testName = "Fill Missing Values with the Last Observed Value")
    public void testExample1() {
        testPipeline(v52, true, false, (aggregation) -> aggregation.pipeline(
                setWindowFields().sortBy(ascending("time")).output(output("price").operator(locf("$price")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/locf/example2
     * 
     */
    @Test(testName = "Use Multiple Fill Methods in a Single Stage")
    public void testExample2() {
        testPipeline(ServerVersion.v52, true, true,
                (aggregation) -> aggregation.pipeline(setWindowFields().sortBy(ascending("time")).output(
                        output("linearFillPrice").operator(linearFill("$price")),
                        output("locfPrice").operator(locf("$price")))));
    }

}
