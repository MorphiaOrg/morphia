package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.BooleanExpressions.and;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.lt;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.switchExpression;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSwitch extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("name")
                        .include("summary",
                                switchExpression()
                                        .branch(gte(avg(field("scores")), value(90)), value("Doing great!"))
                                        .branch(and(
                                                gte(avg(field("scores")), value(80)),
                                                lt(avg(field("scores")), value(90))), value("Doing pretty well."))
                                        .branch(
                                                lt(avg(field("scores")), value(80)), value("Needs improvement."))
                                        .defaultCase(value("No scores found.")))));
    }

}
