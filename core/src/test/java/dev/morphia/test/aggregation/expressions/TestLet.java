package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.VariableExpressions.let;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestLet extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/let/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(project().include("finalTotal",
                        let(multiply("$$total", "$$discounted")).variable("total", add("$price", "$tax"))
                                .variable("discounted", condition("$applyDiscount", 0.9, 1)))));
    }

}
