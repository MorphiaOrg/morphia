package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.Sort;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TimeUnit.HOUR;
import static dev.morphia.aggregation.expressions.WindowExpressions.integral;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;

public class TestIntegral extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(
                setWindowFields()
                        .partitionBy("$powerMeterID")
                        .sortBy(Sort.ascending("timeStamp"))
                        .output(output("powerMeterKilowattHours")
                                .operator(integral("$kilowatts").unit(HOUR))
                                .window()
                                .range("unbounded", "current", HOUR))));
    }

}
