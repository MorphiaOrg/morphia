package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.Sort;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TimeUnit.HOUR;
import static dev.morphia.aggregation.expressions.WindowExpressions.integral;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;

public class TestIntegral extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/integral/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(true).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(setWindowFields().partitionBy("$powerMeterID").sortBy(Sort.ascending("timeStamp"))
                                .output(output("powerMeterKilowattHours").operator(integral("$kilowatts").unit(HOUR))
                                        .window().range("unbounded", "current", HOUR))));
    }

}
