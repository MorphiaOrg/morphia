package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.StringExpressions.strLenCP;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestStrLenCP extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("name")
                        .include("length", strLenCP(field("name")))));
    }

}
