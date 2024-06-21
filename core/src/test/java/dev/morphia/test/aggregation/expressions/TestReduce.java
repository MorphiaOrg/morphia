package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.ArrayExpressions.reduce;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.StringExpressions.concat;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.gt;

public class TestReduce extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/reduce/example1
     * 
     */
    @Test(testName = "Multiplication")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false,
                (aggregation) -> aggregation.pipeline(
                        group(id("$experimentId")).field("probabilityArr", push("$probability")),
                        project().include("description").include("results",
                                reduce("$probabilityArr", 1, multiply("$$value", "$$this")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/reduce/example2
     * 
     */
    @Test(testName = "String Concatenation")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(gt("hobbies", array())),
                project().include("name").include("bio", reduce("$hobbies", "My hobbies include:",
                        concat("$$value", condition(eq("$$value", "My hobbies include:"), " ", ", "), "$$this")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/reduce/example3
     * 
     */
    @Test(testName = "Array Concatenation")
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation
                .pipeline(project().include("collapsed", reduce("$arr", array(), concatArrays("$$value", "$$this")))));
    }

}
