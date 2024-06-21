package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.size;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.SetExpressions.setIntersection;
import static dev.morphia.aggregation.expressions.SystemVariables.DESCEND;
import static dev.morphia.aggregation.expressions.SystemVariables.PRUNE;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Redact.redact;
import static dev.morphia.query.filters.Filters.eq;

public class TestRedact extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/redact/example1
     * 
     */
    @Test(testName = "Evaluate Access at Every Document Level")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(match(eq("year", 2014)),
                redact(condition(gt(size(setIntersection("$tags", array("STLW", "G"))), 0), DESCEND, PRUNE))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/redact/example2
     * 
     */
    @Test(testName = "Exclude All Fields at a Given Level")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(match(eq("status", "A")),
                redact(condition(ComparisonExpressions.eq("$level", 5), PRUNE, DESCEND))));
    }

}
