package dev.morphia.test.query.filters;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.lt;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.query.filters.Filters.expr;

public class TestExpr extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/expr/example1
     */
    @Test(testName = "Compare Two Fields from A Single Document")
    public void testExample1() {
        testQuery((query) -> query.filter(expr(gt("$spent", "$budget"))));
    }

    /**
     * test data: dev/morphia/test/query/filters/expr/example2
     */
    @Test(testName = "Using ``$expr`` With Conditional Statements", enabled = false, description = "parsing bug in Document's json parsing at $cond")
    public void testExample2() {
        testQuery((query) -> query
                .filter(expr(lt(condition(gte("qty", 100), multiply("$price", 0.5), multiply("$price", 0.75)), 5.0))));
    }
}