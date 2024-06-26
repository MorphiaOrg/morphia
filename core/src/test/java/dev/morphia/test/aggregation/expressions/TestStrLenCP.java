package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.strLenCP;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestStrLenCP extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/strLenCP/example1
     * 
     */
    @Test(testName = "Single-Byte and Multibyte Character Set")
    public void testExample1() {
        testPipeline(
                (aggregation) -> aggregation.pipeline(project().include("name").include("length", strLenCP("$name"))));
    }

}
