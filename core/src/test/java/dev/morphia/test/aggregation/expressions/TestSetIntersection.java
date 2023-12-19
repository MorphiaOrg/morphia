package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.BooleanExpressions.*;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.SetExpressions.setIntersection;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSetIntersection extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("flowerFieldA")
                        .include("flowerFieldB")
                        .include("commonToBoth", setIntersection(field("flowerFieldA"), field("flowerFieldB")))));
    }

    @Test
    public void testExample2() {
        // this requires auth and roles configuration which the tests won't have
    }

}
