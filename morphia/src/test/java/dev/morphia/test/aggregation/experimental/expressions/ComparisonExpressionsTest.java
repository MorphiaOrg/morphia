package dev.morphia.test.aggregation.experimental.expressions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.cmp;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.lt;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.lte;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.ne;
import static dev.morphia.aggregation.experimental.expressions.Expressions.value;

public class ComparisonExpressionsTest extends ExpressionsTestBase {

    @Test
    public void testCmp() {
        assertAndCheckDocShape("{ $cmp: [ 5, 10 ] }", cmp(value(5), value(10)), -1);
    }

    @Test
    public void testEq() {
        assertAndCheckDocShape("{ $eq: [ 5, 10 ] }", eq(value(5), value(10)), false);
    }

    @Test
    public void testGt() {
        assertAndCheckDocShape("{ $gt: [ 5, 10 ] }", gt(value(5), value(10)), false);
    }

    @Test
    public void testGte() {
        assertAndCheckDocShape("{ $gte: [ 5, 10 ] }", gte(value(5), value(10)), false);
    }

    @Test
    public void testLt() {
        assertAndCheckDocShape("{ $lt: [ 5, 10 ] }", lt(value(5), value(10)), true);
    }

    @Test
    public void testLte() {
        assertAndCheckDocShape("{ $lte: [ 5, 10 ] }", lte(value(5), value(10)), true);
    }

    @Test
    public void testNe() {
        assertAndCheckDocShape("{ $ne: [ 5, 10 ] }", ne(value(5), value(10)), true);
    }
}
