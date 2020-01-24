package dev.morphia.aggregation.experimental.expressions;

import org.junit.Test;

import static dev.morphia.aggregation.experimental.expressions.Expression.value;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.acos;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.acosh;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.asin;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.asinh;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.atan;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.atan2;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.atanh;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.cos;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.sin;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.tan;

public class TrigonometryExpressionsTest extends ExpressionsTest {

    @Test
    public void testAcos() {
        evaluate("{ $acos: 1 }", acos(value(1)), 0.0);
    }

    @Test
    public void testAcosh() {
        evaluate("{ $acosh: 90 }", acosh(value(90)), 5.192925985263684);
    }

    @Test
    public void testAsin() {
        evaluate("{ $asin: 0 }", asin(value(0)), 0.0);
    }

    @Test
    public void testAsinh() {
        evaluate("{ $asinh: 90 }", asinh(value(90)), 5.192987713658941);
    }

    @Test
    public void testAtan() {
        evaluate("{ $atan: 0 }", atan(value(0)), 0.0);
    }

    @Test
    public void testAtan2() {
        evaluate("{ $atan2: [ 4, 3 ] }", atan2(value(4), value(3)), 0.9272952180016122);
    }

    @Test
    public void testAtanh() {
        evaluate("{ $atanh: 0 }", atanh(value(0)), 0.0);
    }

    @Test
    public void testCos() {
        evaluate("{ $cos: 0}", cos(value(0)), 1.0);
    }

    @Test
    public void testDegreesToRadians() {
        evaluate("{ $degreesToRadians: 90 }", degreesToRadians(value(90)), Math.PI / 2);
    }

    @Test
    public void testRadiansToDegrees() {
        evaluate("{}", radiansToDegrees(value(Math.PI / 2)), 90.0);
    }

    @Test
    public void testSin() {
        evaluate("{}", sin(value(Math.PI / 2)), 1.0);
    }

    @Test
    public void testTan() {
        evaluate("{ $tan: 0 }", tan(value(0)), 0.0);
    }
}
