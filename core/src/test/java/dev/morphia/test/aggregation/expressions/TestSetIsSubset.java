package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.SetExpressions.setIsSubset;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSetIsSubset extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("flowerFieldA")
                        .include("flowerFieldB")
                        .include("AisSubset", setIsSubset(field("flowerFieldA"), field("flowerFieldB")))));
    }

}
