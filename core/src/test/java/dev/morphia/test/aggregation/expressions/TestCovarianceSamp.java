package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.expressions.WindowExpressions.covarianceSamp;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;

public class TestCovarianceSamp extends TemplatedTestBase {

    @BeforeMethod
    public void versionCheck() {
        checkMinServerVersion("5.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/covarianceSamp/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(setWindowFields().partitionBy("$state").sortBy(ascending("orderDate"))
                        .output(output("covarianceSampForState")
                                .operator(covarianceSamp(year("$orderDate"), "$quantity")).window()
                                .documents("unbounded", "current"))));
    }

}
