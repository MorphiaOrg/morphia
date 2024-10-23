package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.Expressions.literal;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestLiteral extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/literal/example1
     * 
     */
    @Test(testName = "Treat ``$`` as a Literal")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("costsOneDollar", eq("$price", literal("$1")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/literal/example2
     * 
     */
    @Test(testName = "Project a New Field with Value ``1``")
    public void testExample2() {
        testPipeline(
                (aggregation) -> aggregation.pipeline(project().include("title").include("editionNumber", literal(1))));
    }

}
