package dev.morphia.test.aggregation.expressions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.asin;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.asinh;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.atan;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.atan2;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.atanh;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.cos;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.cosh;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.sin;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.sinh;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.tan;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.tanh;

public class TrigonometryExpressionsTest extends ExpressionsTestBase {
    @Test
    public void testAsin() {
        assertAndCheckDocShape("{ $asin: 0 }", asin(value(0)), 0.0);
    }

    @Test
    public void testAsinh() {
        assertAndCheckDocShape("{ $asinh: 90 }", asinh(value(90)), 5.192987713658941);
    }

    @Test
    public void testAtan() {
        assertAndCheckDocShape("{ $atan: 0 }", atan(value(0)), 0.0);
    }

    @Test
    public void testAtan2() {
        assertAndCheckDocShape("{ $atan2: [ 4, 3 ] }", atan2(value(4), value(3)), 0.9272952180016122);
    }

    @Test
    public void testAtanh() {
        assertAndCheckDocShape("{ $atanh: 0 }", atanh(value(0)), 0.0);
    }

    @Test
    public void testCos() {
        assertAndCheckDocShape("{ $cos: 0}", cos(value(0)), 1.0);
    }

    @Test
    public void testCosh() {
        assertAndCheckDocShape("{ $cosh: 0}", cosh(value(0)), 1.0);
    }

    @Test
    public void testDegreesToRadians() {
        assertAndCheckDocShape("{ $degreesToRadians: 90 }", degreesToRadians(value(90)), Math.PI / 2);
    }

    @Test
    public void testRadiansToDegrees() {
        assertAndCheckDocShape("{}", radiansToDegrees(value(Math.PI / 2)), 90.0);
    }

    @Test
    public void testSin() {
        assertAndCheckDocShape("{ $sin: 3}", sin(value(3)), 0.1411200080598672D);
    }

    @Test
    public void testSinh() {
        assertAndCheckDocShape("{ $sinh: 3}", sinh(value(3)), 10.017874927409903D);
    }

    @Test
    public void testTan() {
        assertAndCheckDocShape("{ $tan: 0 }", tan(value(0)), 0.0);
    }

    @Test
    public void testTanh() {
        assertAndCheckDocShape("{ $tanh: 0.5 }", tanh(value(0.5)), 0.46211715726000974D);
    }
}
