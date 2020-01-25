package dev.morphia.aggregation.experimental.expressions;

import org.junit.Test;

import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.cmp;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.lt;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.lte;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.ne;
import static dev.morphia.aggregation.experimental.expressions.Expressions.value;

public class ComparisonExpressionsTest extends ExpressionsTest {

    @Test
    public void testCmp() {
        evaluate("{ $cmp: [ 5, 10 ] }", cmp(value(5), value(10)), -1);
    }

    @Test
    public void testEq() {
        evaluate("{ $eq: [ 5, 10 ] }", eq(value(5), value(10)), false);
    }

    @Test
    public void testGt() {
        evaluate("{ $gt: [ 5, 10 ] }", gt(value(5), value(10)), false);
    }

    @Test
    public void testGte() {
        evaluate("{ $gte: [ 5, 10 ] }", gte(value(5), value(10)), false);
    }

    @Test
    public void testLt() {
        evaluate("{ $lt: [ 5, 10 ] }", lt(value(5), value(10)), true);
    }

    @Test
    public void testLte() {
        evaluate("{ $lte: [ 5, 10 ] }", lte(value(5), value(10)), true);
    }

    @Test
    public void testNe() {
        evaluate("{ $ne: [ 5, 10 ] }", ne(value(5), value(10)), true);
    }
}
