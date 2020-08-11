package dev.morphia.test.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.Expressions;
import org.junit.jupiter.api.Test;

public class LiteralExpressionExpressionTest extends ExpressionsTestBase {
    @Test
    public void testLiteral() {
        assertAndCheckDocShape("{ $literal: \"{ $add: [ 2, 3 ] }\" }", Expressions.literal("{ $add: [ 2, 3 ] }"), "{ $add: [ 2, 3 ] }");
    }

}
