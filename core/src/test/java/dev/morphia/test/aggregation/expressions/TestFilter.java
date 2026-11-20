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
     * test data: dev/morphia/test/aggregation/expressions/filter/filterExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testFilterExpressionOperator() {
        testPipeline(new ActionTestOptions(), (aggregation) -> aggregation
                .pipeline(project().include("items", filter("$items", gte("$$item.price", 100)).as("item"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/filter/useTheLimitField
     * 
     */
    @Test
    @DisplayName("Use the limit Field")
    public void testUseTheLimitField() {
        testPipeline(new ActionTestOptions(), (aggregation) -> aggregation
                .pipeline(project().include("items", filter("$items", gte("$$item.price", 100)).as("item").limit(1))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/filter/limitGreaterThanPossibleMatches
     * 
     */
    @Test
    @DisplayName("limit Greater than Possible Matches")
    public void testLimitGreaterThanPossibleMatches() {
        // this example is API incompatible with morphia since limit can only be an int
        // and not a floating point number
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/filter/filterBasedOnStringEqualityMatch
     * 
     */
    @Test
    @DisplayName("Filter Based on String Equality Match")
    public void testFilterBasedOnStringEqualityMatch() {
        testPipeline(new ActionTestOptions(), (aggregation) -> aggregation
                .pipeline(project().include("items", filter("$items", eq("$$item.name", "pen")).as("item"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/filter/filterBasedOnRegularExpressionMatch
     */
    @Test
    @DisplayName("Filter Based on Regular Expression Match")
    public void testFilterBasedOnRegularExpressionMatch() {
        testPipeline(aggregation -> aggregation.pipeline(
                project().include("items", filter("$items", regexMatch("$$item.name").pattern("^p")).as("item"))));
    }
}
