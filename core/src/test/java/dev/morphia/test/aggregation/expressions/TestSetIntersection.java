package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.SetExpressions.setIntersection;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSetIntersection extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/setIntersection/example1
     * 
     */
    @Test(testName = "Elements Array Example")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false,
                (aggregation) -> aggregation
                        .pipeline(project().suppressId().include("flowerFieldA").include("flowerFieldB")
                                .include("commonToBoth", setIntersection("$flowerFieldA", "$flowerFieldB"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/setIntersection/example2
     * 
     */
    @Test(testName = "Retrieve Documents for Roles Granted to the Current User")
    public void testExample2() {
        // this requires auth and roles configuration which the tests won't have
    }

}
