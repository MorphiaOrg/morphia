package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.lt;
import static dev.morphia.aggregation.expressions.MathExpressions.floor;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.Miscellaneous.rand;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Merge.*;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Set.set;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.expr;

public class TestRand extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/rand/generateRandomDataPoints
     * 
     */
    @Test
    @DisplayName("Generate Random Data Points")
    public void testGenerateRandomDataPoints() {
        testPipeline((aggregation) -> aggregation.pipeline(set().field("amount", multiply(rand(), 100)),
                set().field("amount", floor("$amount")), merge("donors")));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/rand/selectRandomItemsFromACollection
     * 
     */
    @Test
    @DisplayName("Select Random Items From a Collection")
    public void testSelectRandomItemsFromACollection() {
        testPipeline((aggregation) -> aggregation.pipeline(match(eq("district", 3)), match(expr(lt(0.5, rand()))),
                project().suppressId().include("name").include("registered")));
    }

}
