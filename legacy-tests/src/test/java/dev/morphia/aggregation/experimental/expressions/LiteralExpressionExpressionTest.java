package dev.morphia.aggregation.experimental.expressions;

import org.junit.Test;

public class LiteralExpressionExpressionTest extends ExpressionsTestBase {
    @Test
    public void testLiteral() {
        assertAndCheckDocShape("{ $literal: \"{ $add: [ 2, 3 ] }\" }", Expressions.literal("{ $add: [ 2, 3 ] }"), "{ $add: [ 2, 3 ] }");
    }

}
