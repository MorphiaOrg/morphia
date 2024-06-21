package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.strLenBytes;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestStrLenBytes extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/strLenBytes/example1
     * 
     */
    @Test(testName = "Single-Byte and Multibyte Character Set")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation
                .pipeline(project().include("name").include("length", strLenBytes("$name"))));
    }

}
