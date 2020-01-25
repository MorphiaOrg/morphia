package dev.morphia.aggregation.experimental.expressions;

import org.junit.Test;

import static dev.morphia.aggregation.experimental.expressions.Expressions.value;

public class MathExpressionTest extends ExpressionsTest {

    @Test
    public void testAbs() {
        evaluate("{ $abs: -1 }", MathExpressions.abs(value(-1)), 1);
        evaluate("{ $abs: 1 }", MathExpressions.abs(value(1)), 1);
        evaluate("{ $abs: null }", MathExpressions.abs(null), null);
    }

    @Test
    public void testAdd() {
        evaluate("{ $add: [ 4, 5 ] } ", MathExpressions.add(value(4), value(5)), 9);
    }

    @Test
    public void testCeil() {
        evaluate("{ $ceil: 7.80 }", MathExpressions.ceil(value(7.80)), 8.0);
    }

    @Test
    public void testDivide() {
        evaluate("{ $divide: [ 16, 8 ] } }", MathExpressions.divide(value(16), value(8)), 2.0);
    }

    @Test
    public void testExp() {
        evaluate("{ $exp: 0 } ", MathExpressions.exp(value(0)), 1.0);
    }

    @Test
    public void testFloor() {
        evaluate("{ $floor: 1.5 }", MathExpressions.floor(value(1.5)), 1.0);
    }

    @Test
    public void testLn() {
        evaluate("{ $ln: 1 }", MathExpressions.ln(value(1)), 0.0);
    }

    @Test
    public void testLog() {
        evaluate("{ $log: [ 100, 10 ] }", MathExpressions.log(value(100), value(10)), 2.0);
    }

    @Test
    public void testLog10() {
        evaluate("{ $log10: 100 }", MathExpressions.log10(value(100)), 2.0);
    }

    @Test
    public void testMod() {
        evaluate("{ $mod: [ 12, 5 ] }", MathExpressions.mod(value(12), value(5)), 2);
    }

    @Test
    public void testMultiply() {
        evaluate("{ $multiply: [ 3, 4, 5 ] }", MathExpressions.multiply(value(3), value(4), value(5)), 60);
    }

    @Test
    public void testPow() {
        evaluate("{ $pow: [ 5, 2 ] }", MathExpressions.pow(value(5), value(2)), 25);
    }

    @Test
    public void testRound() {
        evaluate("{ $round: [ 19.25, 1 ] }", MathExpressions.round(value(19.25), value(1)), 19.2);
    }

    @Test
    public void testSqrt() {
        evaluate("{ $sqrt: 25 }", MathExpressions.sqrt(value(25)), 5.0);
    }

    @Test
    public void testSubtract() {
        evaluate("{ $subtract: [ { $add: [ 4 , 5 ] }, 6 ] }",
            MathExpressions.subtract(MathExpressions.add(value(4), value(5)), value(6)), 3);
    }

    @Test
    public void trunc() {
        evaluate("{ $trunc: [ 7.85, 1 ] }", MathExpressions.trunc(value(7.85), value(1)), 7.8);
    }

}
