package dev.morphia.test.aggregation.experimental.expressions;

import com.github.zafarkhaja.semver.Version;
import dev.morphia.aggregation.experimental.expressions.MathExpressions;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.experimental.expressions.Expressions.value;
import static dev.morphia.aggregation.experimental.expressions.MathExpressions.trunc;

public class MathExpressionTest extends ExpressionsTestBase {

    @Test
    public void testAbs() {
        assertAndCheckDocShape("{ $abs: -1 }", MathExpressions.abs(value(-1)), 1);
        assertAndCheckDocShape("{ $abs: 1 }", MathExpressions.abs(value(1)), 1);
        assertAndCheckDocShape("{ $abs: null }", MathExpressions.abs(null), null);
    }

    @Test
    public void testAdd() {
        assertAndCheckDocShape("{ $add: [ 4, 5 ] } ", MathExpressions.add(value(4), value(5)), 9);
    }

    @Test
    public void testCeil() {
        assertAndCheckDocShape("{ $ceil: 7.80 }", MathExpressions.ceil(value(7.80)), 8.0);
    }

    @Test
    public void testDivide() {
        assertAndCheckDocShape("{ $divide: [ 16, 8 ] } }", MathExpressions.divide(value(16), value(8)), 2.0);
    }

    @Test
    public void testExp() {
        assertAndCheckDocShape("{ $exp: 0 } ", MathExpressions.exp(value(0)), 1.0);
    }

    @Test
    public void testFloor() {
        assertAndCheckDocShape("{ $floor: 1.5 }", MathExpressions.floor(value(1.5)), 1.0);
    }

    @Test
    public void testLn() {
        assertAndCheckDocShape("{ $ln: 1 }", MathExpressions.ln(value(1)), 0.0);
    }

    @Test
    public void testLog() {
        assertAndCheckDocShape("{ $log: [ 100, 10 ] }", MathExpressions.log(value(100), value(10)), 2.0);
    }

    @Test
    public void testLog10() {
        assertAndCheckDocShape("{ $log10: 100 }", MathExpressions.log10(value(100)), 2.0);
    }

    @Test
    public void testMod() {
        assertAndCheckDocShape("{ $mod: [ 12, 5 ] }", MathExpressions.mod(value(12), value(5)), 2);
    }

    @Test
    public void testMultiply() {
        assertAndCheckDocShape("{ $multiply: [ 3, 4, 5 ] }", MathExpressions.multiply(value(3), value(4), value(5)), 60);
    }

    @Test
    public void testPow() {
        assertAndCheckDocShape("{ $pow: [ 5, 2 ] }", MathExpressions.pow(value(5), value(2)), 25);
    }

    @Test
    public void testRound() {
        checkMinServerVersion(4.2);
        assertAndCheckDocShape("{ $round: [ 19.25, 1 ] }", MathExpressions.round(value(19.25), value(1)), 19.2);
    }

    @Test
    public void testSqrt() {
        assertAndCheckDocShape("{ $sqrt: 25 }", MathExpressions.sqrt(value(25)), 5.0);
    }

    @Test
    public void testSubtract() {
        assertAndCheckDocShape("{ $subtract: [ { $add: [ 4 , 5 ] }, 6 ] }",
            MathExpressions.subtract(MathExpressions.add(value(4), value(5)), value(6)), 3);
    }

    @Test
    public void testTrunc() {
        if (getServerVersion().greaterThanOrEqualTo(Version.forIntegers(4, 2))) {
            assertAndCheckDocShape("{ $trunc: [ 7.85, 1 ] }", trunc(value(7.85), value(1)), 7.8);
        } else {
            assertAndCheckDocShape("{ $trunc: 7.85 }", trunc(value(7.85), null), 7.0);
        }
    }
}
