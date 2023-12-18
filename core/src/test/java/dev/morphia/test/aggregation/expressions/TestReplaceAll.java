package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.StringExpressions.replaceAll;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestReplaceAll extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("item",
                                replaceAll(
                                        field("item"),
                                        value("blue paint"),
                                        value("red paint")))));
    }

}
