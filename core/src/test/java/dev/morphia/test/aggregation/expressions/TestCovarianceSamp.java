package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.expressions.WindowExpressions.covarianceSamp;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;

public class TestCovarianceSamp extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/covarianceSamp/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(setWindowFields().partitionBy("$state").sortBy(ascending("orderDate"))
                                .output(output("covarianceSampForState")
                                        .operator(covarianceSamp(year("$orderDate"), "$quantity")).window()
                                        .documents("unbounded", "current"))));
    }

}
