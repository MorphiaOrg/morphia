package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.elementAt;
import static dev.morphia.aggregation.expressions.ArrayExpressions.zip;
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

}
