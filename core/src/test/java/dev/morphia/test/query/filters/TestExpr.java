package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.lt;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.query.filters.Filters.expr;

public class TestExpr extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/expr/example1
     */
    @Test
    @DisplayName("Compare Two Fields from A Single Document")
    public void testExample1() {
        testQuery((query) -> query.filter(expr(gt("$spent", "$budget"))));
    }

    /**
     * test data: dev/morphia/test/query/filters/expr/example2
     */
    @Disabled("parsing bug in Document's json parsing at $cond")
    @Test
    @DisplayName("Using ``$expr`` With Conditional Statements")
    public void testExample2() {
        testQuery((query) -> query
                .filter(expr(lt(condition(gte("qty", 100), multiply("$price", 0.5), multiply("$price", 0.75)), 5.0))));
    }
}