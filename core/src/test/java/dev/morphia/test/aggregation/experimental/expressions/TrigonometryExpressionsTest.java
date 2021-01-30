package dev.morphia.test.aggregation.experimental.expressions;

import com.github.zafarkhaja.semver.Version;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.experimental.expressions.Expressions.value;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.acos;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.acosh;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.asin;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.asinh;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.atan;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.atan2;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.atanh;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.cos;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.cosh;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.sin;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.sinh;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.tan;
import static dev.morphia.aggregation.experimental.expressions.TrigonometryExpressions.tanh;

public class TrigonometryExpressionsTest extends ExpressionsTestBase {
    @BeforeMethod
    public void before() {
        checkMinServerVersion(4.2);
    }

    @Test
    public void testAcos() {
        assertAndCheckDocShape("{ $acos: 1 }", acos(value(1)), 0.0);
    }

    @Test
    public void testAcosh() {
        assertAndCheckDocShape("{ $acosh: 90 }", acosh(value(90)), 5.192925985263684);
    }

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
        checkMinServerVersion(Version.valueOf("4.2.0"));
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
        checkMinServerVersion(Version.valueOf("4.2.0"));
        assertAndCheckDocShape("{ $sinh: 3}", sinh(value(3)), 10.017874927409903D);
    }

    @Test
    public void testTan() {
        assertAndCheckDocShape("{ $tan: 0 }", tan(value(0)), 0.0);
    }

    @Test
    public void testTanh() {
        checkMinServerVersion(Version.valueOf("4.2.0"));
        assertAndCheckDocShape("{ $tanh: 0.5 }", tanh(value(0.5)), 0.46211715726000974D);
    }
}
