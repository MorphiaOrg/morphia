package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.expressions.Expressions.filter;
import static dev.morphia.aggregation.expressions.StringExpressions.regexMatch;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestFilter extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.2.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/filter/example1
     * 
     */
    @Test
    @DisplayName("main")
    public void testExample1() {
        testPipeline(new ActionTestOptions(), (aggregation) -> aggregation
                .pipeline(project().include("items", filter("$items", gte("$$item.price", 100)).as("item"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/filter/example2
     * 
     */
    @Test
    @DisplayName("Use the limit Field")
    public void testExample2() {
        testPipeline(new ActionTestOptions(), (aggregation) -> aggregation
                .pipeline(project().include("items", filter("$items", gte("$$item.price", 100)).as("item").limit(1))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/filter/example3
     * 
     */
    @Test
    @DisplayName("limit Greater than Possible Matches")
    public void testExample3() {
        // this example is API incompatible with morphia since limit can only be an int
        // and not a floating point number
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/filter/example4
     * 
     */
    @Test
    @DisplayName("Filter Based on String Equality Match")
    public void testExample4() {
        testPipeline(new ActionTestOptions(), (aggregation) -> aggregation
                .pipeline(project().include("items", filter("$items", eq("$$item.name", "pen")).as("item"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/filter/example5
     */
    @Test
    @DisplayName("Filter Based on Regular Expression Match")
    public void testExample5() {
        testPipeline(aggregation -> aggregation.pipeline(
                project().include("items", filter("$items", regexMatch("$$item.name").pattern("^p")).as("item"))));
    }
}
