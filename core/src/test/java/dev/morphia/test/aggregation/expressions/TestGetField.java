package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.lte;
import static dev.morphia.aggregation.expressions.Expressions.literal;
import static dev.morphia.aggregation.expressions.Miscellaneous.getField;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.expr;

public class TestGetField extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/getField/example1
     * 
     */
    @Test(testName = "Query Fields that Contain Periods (``.``)")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(match(expr(gt(getField("price.usd"), 200)))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/getField/example2
     * 
     */
    @Test(testName = "Query Fields that Start with a Dollar Sign (``$``)")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(match(expr(gt(getField(literal("$price")), 200)))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/getField/example3
     * 
     */
    @Test(testName = "Query a Field in a Sub-document")
    public void testExample3() {
        testPipeline((aggregation) -> aggregation
                .pipeline(match(expr(lte(getField(literal("$small")).input("$quantity"), 20)))));
    }

}
