package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.BooleanExpressions.*;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.SetExpressions.setIntersection;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.expr;

public class TestSetIntersection extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("flowerFieldA")
                        .include("flowerFieldB")
                        .include("commonToBoth", setIntersection(field("flowerFieldA"), field("flowerFieldB")))));
    }

    @Test
    public void testExample2() {
        skipDataCheck = true;
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(expr(not(eq(
                        setIntersection(field("allowedRoles"), value("$$USER_ROLES.role")),
                        array()))))));
    }

}
