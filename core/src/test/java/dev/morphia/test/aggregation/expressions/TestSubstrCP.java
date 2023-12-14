package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.subtract;
import static dev.morphia.aggregation.expressions.StringExpressions.strLenCP;
import static dev.morphia.aggregation.expressions.StringExpressions.substrCP;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSubstrCP extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("item")
                        .include("yearSubstring", substrCP(field("quarter"), 0, 2))
                        .include("quarterSubtring", substrCP(field("quarter"), value(2),
                                subtract(strLenCP(field("quarter")), value(2))))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("name")
                        .include("menuCode", substrCP(field("name"), 0, 3))));
    }

}
