package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.*;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.MathExpressions.trunc;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestMap extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/map/example1
     * 
     */
    @Test(testName = "Add to Each Element of an Array")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation
                .pipeline(project().include("adjustedGrades", map("$quizzes", add("$$grade", 2)).as("grade"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/map/example2
     * 
     */
    @Test(testName = "Truncate Each Array Element")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, true, true,
                (aggregation) -> aggregation.pipeline(project().include("city", "$city").include("integerValues",
                        map("$distances", trunc("$$decimalValue")).as("decimalValue"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/map/example3
     * 
     */
    @Test(testName = "Convert Celsius Temperatures to Fahrenheit")
    public void testExample3() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(addFields().field("tempsF",
                map("$tempsC", add(multiply("$$tempInCelsius", 1.8), 32)).as("tempInCelsius"))));
    }
}
