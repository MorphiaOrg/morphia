package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.elementAt;
import static dev.morphia.aggregation.expressions.ArrayExpressions.range;
import static dev.morphia.aggregation.expressions.ArrayExpressions.size;
import static dev.morphia.aggregation.expressions.ArrayExpressions.zip;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.expressions.Expressions.filter;
import static dev.morphia.aggregation.expressions.VariableExpressions.let;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestZip extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/zip/example1
     * 
     */
    @Test(testName = "Matrix Transposition")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(project().suppressId().include("transposed",
                zip(elementAt("$matrix", 0), elementAt("$matrix", 1), elementAt("$matrix", 2)))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/zip/example2
     * 
     */
    @Test(testName = "Filtering and Preserving Indexes")
    public void testExample2() {
        testPipeline((aggregation) -> {
            return aggregation.pipeline(project().suppressId().include("pages",
                    filter(zip("$pages", range(0, size("$pages"))),
                            let(gte("$$page.reviews", 1)).variable("page", elementAt("$$pageWithIndex", 0)))
                            .as("pageWithIndex")));
        });
    }

}
