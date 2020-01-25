package dev.morphia.aggregation.experimental.expressions;

import org.junit.Test;

import static dev.morphia.aggregation.experimental.expressions.BooleanExpressions.and;
import static dev.morphia.aggregation.experimental.expressions.BooleanExpressions.not;
import static dev.morphia.aggregation.experimental.expressions.BooleanExpressions.or;
import static dev.morphia.aggregation.experimental.expressions.Expressions.value;

public class BooleanExpressionsTest extends ExpressionsTestBase {

    @Test
    public void testAnd() {
        evaluate("{ $and: [ 1, \"green\" ] }", and(value(1), value("green")), true);
    }

    @Test
    public void testNot() {
        evaluate("{ $not: [ true ] }", not(value(true)), false);
    }

    @Test
    public void testOr() {
        evaluate("{ $or: [ true, false ] }", or(value(true), value(false)), true);
    }
}