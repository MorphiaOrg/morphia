package dev.morphia.aggregation.experimental.expressions;

import org.junit.Test;

import static dev.morphia.aggregation.experimental.expressions.Expression.value;
import static dev.morphia.aggregation.experimental.expressions.Expression.nullExpression;
import static dev.morphia.aggregation.experimental.expressions.MathExpression.*;

public class MathExpressionTest extends ExpressionsTest {

    @Test
    public void testAbs() {
        evaluate("{ $abs: -1 }", abs(value(-1)), 1);
        evaluate("{ $abs: 1 }", abs(value(1)), 1);
        evaluate("{ $abs: null }", abs(nullExpression()), null);
    }

    @Test
    public void testAdd() {
        evaluate("{ $add: [ 4, 5 ] } ", add(value(4), value(5)), 9);
    }

    @Test
    public void testCeil() {
        evaluate("{ $ceil: 7.80 }", ceil(value(7.80)), 8.0);
    }

    @Test
    public void testDivide() {
        evaluate("{ $divide: [ 16, 8 ] } }", divide(value(16), value(8)), 2.0);
    }

    @Test
    public void testExp() {
        evaluate("{ $exp: 0 } ", exp(value(0)), 1.0);
    }

    @Test
    public void testFloor() {
        evaluate("{ $floor: 1.5 }", floor(value(1.5)), 1.0);
    }

    @Test
    public void testLn() {
        evaluate("{ $ln: 1 }", ln(value(1)), 0.0);
    }

    @Test
    public void testLog() {
        evaluate("{ $log: [ 100, 10 ] }", log(value(100), value(10)), 2.0);
    }

    @Test
    public void testLog10() {
        evaluate("{ $log10: 100 }", log10(value(100)), 2.0);
    }

    @Test
    public void testMod() {
        evaluate("{ $mod: [ 12, 5 ] }", mod(value(12), value(5)), 2);
    }

    @Test
    public void testMultiply() {
        evaluate("{ $multiply: [ 3, 4, 5 ] }", multiply(value(3), value(4), value(5)), 60);
    }

    @Test
    public void testPow() {
        evaluate("{ $pow: [ 5, 2 ] }", pow(value(5), value(2)), 25);
    }

    @Test
    public void testRound() {
        evaluate("{ $round: [ 19.25, 1 ] }", round(value(19.25), value(1)), 19.2);
    }

    @Test
    public void testSqrt() {
        evaluate("{ $sqrt: 25 }", sqrt(value(25)), 5.0);
    }

    @Test
    public void testSubtract() {
        evaluate("{ $subtract: [ { $add: [ 4 , 5 ] }, 6 ] }",
            subtract(add(value(4), value(5)), value(6)), 3);
    }

    @Test
    public void trunc() {
        evaluate("{ $trunc: [ 7.85, 1 ] }", MathExpression.trunc(value(7.85), value(1)), 7.8);
    }

}
