package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.Expressions;

import org.testng.annotations.Test;

public class LiteralExpressionExpressionTest extends ExpressionsTestBase {
    @Test
    public void testLiteral() {
        assertAndCheckDocShape("{ $literal: \"{ $add: [ 2, 3 ] }\" }", Expressions.literal("{ $add: [ 2, 3 ] }"), "{ $add: [ 2, 3 ] }");
    }

}
