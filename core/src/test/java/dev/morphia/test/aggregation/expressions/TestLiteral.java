package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.Expressions.literal;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestLiteral extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/literal/treatAsALiteral
     * 
     */
    @Test
    @DisplayName("Treat ``$`` as a Literal")
    public void testTreatAsALiteral() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("costsOneDollar", eq("$price", literal("$1")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/literal/projectANewFieldWithValue1
     * 
     */
    @Test
    @DisplayName("Project a New Field with Value ``1``")
    public void testProjectANewFieldWithValue1() {
        testPipeline(
                (aggregation) -> aggregation.pipeline(project().include("title").include("editionNumber", literal(1))));
    }

}
