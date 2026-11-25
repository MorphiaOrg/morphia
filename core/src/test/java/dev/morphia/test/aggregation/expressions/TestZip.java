package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
     * test data: dev/morphia/test/aggregation/expressions/zip/matrixTransposition
     * 
     */
    @Test
    @DisplayName("Matrix Transposition")
    public void testMatrixTransposition() {
        testPipeline((aggregation) -> aggregation.pipeline(project().suppressId().include("transposed",
                zip(elementAt("$matrix", 0), elementAt("$matrix", 1), elementAt("$matrix", 2)))));
    }

    @Test
    @DisplayName("Filtering and Preserving Indexes")
    public void testFilteringAndPreservingIndexes() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().suppressId()
                        .include("pages",
                                filter(zip("$pages", range(0, size("$pages"))),
                                        let(gte("$$page.reviews", 1)).variable("page", elementAt("$$pageWithIndex", 0)))
                                        .as("pageWithIndex"))));
    }
}
