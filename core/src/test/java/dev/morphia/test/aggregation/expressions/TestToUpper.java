package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.toUpper;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestToUpper extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("item", toUpper("$item"))
                        .include("description", toUpper("$description"))));
    }

}
