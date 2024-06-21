package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.expressions.StringExpressions;
import dev.morphia.aggregation.stages.Fill.Method;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ConditionalExpressions.ifNull;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.TypeExpressions.toBool;
import static dev.morphia.aggregation.stages.Fill.fill;
import static dev.morphia.aggregation.stages.Set.set;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.test.DriverVersion.v42;
import static dev.morphia.test.ServerVersion.v53;

public class TestFill extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/fill/example1
     * 
     */
    @Test(testName = "Fill Missing Field Values with a Constant Value")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(v53).minDriver(v42).removeIds(true),
                aggregation -> aggregation
                        .pipeline(fill().field("bootsSold", 0).field("sandalsSold", 0).field("sneakersSold", 0)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/fill/example2
     * 
     */
    @Test(testName = "Fill Missing Field Values with Linear Interpolation")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(v53).removeIds(true),
                aggregation -> aggregation.pipeline(fill().sortBy(ascending("time")).field("price", Method.LINEAR)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/fill/example3
     * 
     */
    @Test(testName = "Fill Missing Field Values Based on the Last Observed Value")
    public void testExample3() {
        testPipeline(new ActionTestOptions().serverVersion(v53).minDriver(v42).removeIds(true),
                aggregation -> aggregation.pipeline(fill().sortBy(ascending("date")).field("score", Method.LOCF)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/fill/example4
     * 
     */
    @Test(testName = "Fill Data for Distinct Partitions")
    public void testExample4() {
        testPipeline(new ActionTestOptions().serverVersion(v53).minDriver(v42).removeIds(true),
                aggregation -> aggregation.pipeline(fill().sortBy(ascending("date"))
                        .partitionBy(document("restaurant", "$restaurant")).field("score", Method.LOCF)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/fill/example5
     * 
     */
    @Test(testName = "Indicate if a Field was Populated Using ``$fill``")
    public void testExample5() {
        testPipeline(new ActionTestOptions().serverVersion(v53).removeIds(true).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(
                                set().field("valueExisted",
                                        ifNull().target(toBool(StringExpressions.toString("$score")))
                                                .replacement(false)),
                                fill().sortBy(ascending("date")).field("score", Method.LOCF)));
    }

}
