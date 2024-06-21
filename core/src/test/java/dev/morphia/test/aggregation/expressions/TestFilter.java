package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.expressions.Expressions.filter;
import static dev.morphia.aggregation.expressions.StringExpressions.regexMatch;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.test.ServerVersion.v52;

public class TestFilter extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/filter/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(v52).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(project().include("items", filter("$items", gte("$$item.price", 100)).as("item"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/filter/example2
     * 
     */
    @Test(testName = "Use the limit Field")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(v52).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(
                        project().include("items", filter("$items", gte("$$item.price", 100)).as("item").limit(1))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/filter/example3
     * 
     */
    @Test(testName = "limit Greater than Possible Matches")
    public void testExample3() {
        // this example is API incompatible with morphia since limit can only be an int
        // and not a floating point number
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/filter/example4
     * 
     */
    @Test(testName = "Filter Based on String Equality Match")
    public void testExample4() {
        testPipeline(new ActionTestOptions().serverVersion(v52).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(project().include("items", filter("$items", eq("$$item.name", "pen")).as("item"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/filter/example5
     */
    @Test(testName = "Filter Based on Regular Expression Match")
    public void testExample5() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                aggregation -> aggregation.pipeline(project().include("items",
                        filter("$items", regexMatch("$$item.name").pattern("^p")).as("item"))));
    }
}
