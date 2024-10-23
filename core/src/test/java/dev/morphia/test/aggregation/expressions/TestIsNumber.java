package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

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

public class TestIsNumber extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/isNumber/example1
     * 
     */
    @Test(testName = "Use $isNumber to Check if a Field is Numeric")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("isNumber", isNumber("$reading")).field("hasType", type("$reading"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/isNumber/example2
     * 
     */
    @Test(testName = "Conditionally Modify Fields using $isNumber")
    public void testExample2() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(
                        addFields().field("points",
                                condition(isNumber("$grade"), "$grade",
                                        switchExpression().branch(eq("$grade", "A"), 4.0).branch(eq("$grade", "B"), 3.0)
                                                .branch(eq("$grade", "C"), 2.0).branch(eq("$grade", "D"), 1.0)
                                                .branch(eq("$grade", "F"), 0.0))),
                        group(id("$student_id")).field("GPA", avg("$points"))));
    }

}
