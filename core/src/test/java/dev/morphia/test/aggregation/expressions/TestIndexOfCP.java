package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.indexOfCP;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestIndexOfCP extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/indexOfCP/indexOfCpExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testIndexOfCpExpressionOperator() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(project().include("cpLocation", indexOfCP("$item", "foo"))));
    }

}
