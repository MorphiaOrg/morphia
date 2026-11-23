package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.*;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.MathExpressions.trunc;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestMap extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/map/addToEachElementOfAnArray
     * 
     */
    @Test
    @DisplayName("Add to Each Element of an Array")
    public void testAddToEachElementOfAnArray() {
        testPipeline(new ActionTestOptions().removeIds(true), (aggregation) -> aggregation
                .pipeline(project().include("adjustedGrades", map("$quizzes", add("$$grade", 2)).as("grade"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/map/truncateEachArrayElement
     * 
     */
    @Test
    @DisplayName("Truncate Each Array Element")
    public void testTruncateEachArrayElement() {
        testPipeline(new ActionTestOptions().removeIds(true),
                (aggregation) -> aggregation.pipeline(project().include("city", "$city").include("integerValues",
                        map("$distances", trunc("$$decimalValue")).as("decimalValue"))));
    }

}
