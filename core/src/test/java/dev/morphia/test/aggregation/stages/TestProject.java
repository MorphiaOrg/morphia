package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.StringExpressions.substrBytes;
import static dev.morphia.aggregation.expressions.SystemVariables.REMOVE;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestProject extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/stages/project/example1
     * 
     */
    @Test(testName = "Include Specific Fields in Output Documents")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(project().include("title").include("author")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/example2
     * 
     */
    @Test(testName = "Suppress ``_id`` Field in the Output Documents")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(project().suppressId().include("title").include("author")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/example3
     * 
     */
    @Test(testName = "Exclude Fields from Output Documents")
    public void testExample3() {
        skipDataCheck();
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(project().exclude("lastModified")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/example4
     * 
     */
    @Test(testName = "Conditionally Exclude Fields")
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation
                        .pipeline(project().include("title").include("author.first").include("author.last").include(
                                "author.middle", condition(eq("", "$author.middle"), REMOVE, "$author.middle"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/example5
     * 
     */
    @Test(testName = "Include Specific Fields from Embedded Documents")
    public void testExample5() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(project().include("stop.title")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/example7
     *
     */
    @Test(testName = "Include Specific Fields from Embedded Documents")
    public void testExample6() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(project().include("stop.title")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/example6
     *
     */
    @Test(testName = "Include Computed Fields")
    public void testExample7() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(project().include("title")
                .include("isbn", document().field("prefix", substrBytes("$isbn", 0, 3))
                        .field("group", substrBytes("$isbn", 3, 2)).field("publisher", substrBytes("$isbn", 5, 4))
                        .field("title", substrBytes("$isbn", 9, 3)).field("checkDigit", substrBytes("$isbn", 12, 1)))
                .include("lastName", "$author.last").include("copiesSold", "$copies")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/example8
     * 
     */
    @Test(testName = "Array Indexes are Unsupported")
    public void testExample8() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(project().suppressId().include("x", "$name")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/example9
     * 
     */
    @Test(testName = "Array Indexes are Unsupported")
    public void testExample9() {
        // unsupported multiple examples here
        /*
         * testPipeline(ServerVersion.ANY, false, true, (aggregation) ->
         * aggregation.pipeline( project() .suppressId() .include("x", "$name")));
         */
    }

}
