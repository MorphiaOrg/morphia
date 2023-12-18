package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.StringExpressions.toLower;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestToLower extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("item", toLower(field("item")))
                        .include("description", toLower(field("description")))));
    }

}
