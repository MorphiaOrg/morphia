package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.Sort;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.TimeUnit.HOUR;
import static dev.morphia.aggregation.expressions.WindowExpressions.integral;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;

public class TestIntegral extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/integral/example1
     * 
     */
    @Test
    @DisplayName("main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().removeIds(true),
                (aggregation) -> aggregation
                        .pipeline(setWindowFields().partitionBy("$powerMeterID").sortBy(Sort.ascending("timeStamp"))
                                .output(output("powerMeterKilowattHours").operator(integral("$kilowatts").unit(HOUR))
                                        .window().range("unbounded", "current", HOUR))));
    }

}
