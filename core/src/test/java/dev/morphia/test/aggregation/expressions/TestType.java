package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.TypeExpressions.type;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestType extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/type/typeExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testTypeExpressionOperator() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("a", type("$a"))));
    }

    @Test
    @DisplayName("Existence Check")
    public void testExistenceCheck() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("a", condition(eq(type("$a"), "missing"), array(), "$a"))));
    }
}
