package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.subtract;
import static dev.morphia.aggregation.expressions.StringExpressions.strLenBytes;
import static dev.morphia.aggregation.expressions.StringExpressions.substrBytes;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSubstrBytes extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/substrBytes/example1
     * 
     */
    @Test(testName = "Single-Byte Character Set")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(
                        project().include("item").include("yearSubstring", substrBytes("$quarter", 0, 2)).include(
                                "quarterSubtring", substrBytes("$quarter", 2, subtract(strLenBytes("$quarter"), 2)))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/substrBytes/example2
     * 
     */
    @Test(testName = "Single-Byte and Multibyte Character Set")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(project().include("name").include("menuCode", substrBytes("$name", 0, 3))));
    }

}
