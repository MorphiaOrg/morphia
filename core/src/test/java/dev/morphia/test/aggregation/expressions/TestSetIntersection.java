package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.SetExpressions.setIntersection;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSetIntersection extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/setIntersection/example1
     * 
     */
    @Test
    @DisplayName("Elements Array Example")
    public void testExample1() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation
                        .pipeline(project().suppressId().include("flowerFieldA").include("flowerFieldB")
                                .include("commonToBoth", setIntersection("$flowerFieldA", "$flowerFieldB"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/setIntersection/example2
     * 
     */
    @Test
    @DisplayName("Retrieve Documents for Roles Granted to the Current User")
    public void testExample2() {
        // this requires auth and roles configuration which the tests won't have
    }

}
