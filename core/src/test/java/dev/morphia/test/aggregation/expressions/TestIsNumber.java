package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.switchExpression;
import static dev.morphia.aggregation.expressions.TypeExpressions.isNumber;
import static dev.morphia.aggregation.expressions.TypeExpressions.type;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;

public class TestIsNumber extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("isNumber", isNumber("$reading"))
                        .field("type", type("$reading"))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("points",
                                condition(isNumber("$grade"),
                                        "$grade",
                                        switchExpression()
                                                .branch(eq("$grade", "A"), 4.0)
                                                .branch(eq("$grade", "B"), 3.0)
                                                .branch(eq("$grade", "C"), 2.0)
                                                .branch(eq("$grade", "D"), 1.0)
                                                .branch(eq("$grade", "F"), 0.0))),
                group(id("$student_id"))
                        .field("GPA", avg("$points"))));
    }

}
