package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
     * test data: dev/morphia/test/aggregation/expressions/isNumber/useIsNumberToCheckIfAFieldIsNumeric
     * 
     */
    @Test
    @DisplayName("Use $isNumber to Check if a Field is Numeric")
    public void testUseIsNumberToCheckIfAFieldIsNumeric() {
        testPipeline((aggregation) -> aggregation
                .pipeline(addFields().field("isNumber", isNumber("$reading")).field("hasType", type("$reading"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/isNumber/conditionallyModifyFieldsUsingIsNumber
     * 
     */
    @Test
    @DisplayName("Conditionally Modify Fields using $isNumber")
    public void testConditionallyModifyFieldsUsingIsNumber() {
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
