package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.BooleanExpressions.not;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.SetExpressions.setIntersection;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.expr;

public class TestSetIntersection extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/setIntersection/elementsArrayExample
     * 
     */
    @Test
    @DisplayName("Elements Array Example")
    public void testElementsArrayExample() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().suppressId().include("flowerFieldA").include("flowerFieldB")
                        .include("commonToBoth", setIntersection("$flowerFieldA", "$flowerFieldB"))));
    }

    @Test
    @DisplayName("Retrieve the documents")
    public void testRetrieveTheDocuments() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(expr(not(eq(setIntersection("$allowedRoles", "$$USER_ROLES.role"), array()))))));
    }
}
