package dev.morphia.aggregation.experimental.expressions;

import org.junit.Test;

public class LiteralExpressionTest extends ExpressionsTest {
    @Test
    public void testLiteral() {
        evaluate("{ $literal: \"{ $add: [ 2, 3 ] }\" }", Expressions.literal("{ $add: [ 2, 3 ] }"), "{ $add: [ 2, 3 ] }");
    }

}
