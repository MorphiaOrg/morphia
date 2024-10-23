package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.expMovingAvg;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.*;

public class TestExpMovingAvg extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/expMovingAvg/example1
     * 
     */
    @Test(testName = "Exponential Moving Average Using ``N``")
    public void testExample1() {
        testPipeline(new ActionTestOptions().removeIds(true),
                (aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$stock").sortBy(ascending("date"))
                        .output(output("expMovingAvgForStock").operator(expMovingAvg("$price", 2)))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/expMovingAvg/example2
     * 
     */
    @Test(testName = "Exponential Moving Average Using ``alpha``")
    public void testExample2() {
        testPipeline(new ActionTestOptions().removeIds(true),
                (aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$stock").sortBy(ascending("date"))
                        .output(output("expMovingAvgForStock").operator(expMovingAvg("$price", 0.75)))));
    }
}
