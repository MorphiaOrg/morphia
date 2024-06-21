package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.stdDevSamp;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Sample.sample;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.*;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.*;

public class TestStdDevSamp extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/stdDevSamp/example1
     * 
     */
    @Test(testName = "Use in ``$group`` Stage")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(sample(100),
                group(id(null)).field("ageStdDev", stdDevSamp("$age"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/stdDevSamp/example2
     * 
     */
    @Test(testName = "Use in ``$setWindowFields`` Stage")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                        .sortBy(ascending("orderDate")).output(output("stdDevSampQuantityForState")
                                .operator(stdDevSamp("$quantity")).window().documents("unbounded", "current"))));
    }

}
