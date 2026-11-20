package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ConditionalExpressions.ifNull;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestIfNull extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/ifNull/singleInputExpression
     * 
     */
    @Test
    @DisplayName("Single Input Expression")
    public void testSingleInputExpression() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("item").include("description",
                ifNull().target("$description").replacement("Unspecified"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/ifNull/multipleInputExpressions
     * 
     */
    @Test
    @DisplayName("Multiple Input Expressions")
    public void testMultipleInputExpressions() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("item").include("value",
                ifNull().input("$description", "$quantity").replacement("Unspecified"))));
    }

}
