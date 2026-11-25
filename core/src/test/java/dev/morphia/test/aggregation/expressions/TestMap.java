package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.*;
import static dev.morphia.aggregation.expressions.ArrayExpressions.map;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.MathExpressions.trunc;
import static dev.morphia.aggregation.expressions.StringExpressions.strLenCP;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.gt;

public class TestMap extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/map/addToEachElementOfAnArray
     * 
     */
    @Test
    @DisplayName("Add to Each Element of an Array")
    public void testAddToEachElementOfAnArray() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("adjustedGrades", map("$quizzes", add("$$grade", 2)).as("grade"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/map/truncateEachArrayElement
     * 
     */
    @Test
    @DisplayName("Truncate Each Array Element")
    public void testTruncateEachArrayElement() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("city", "$city").include("integerValues",
                map("$distances", trunc("$$decimalValue")).as("decimalValue"))));
    }

    @Test
    @DisplayName("Apply Arithmetic Operators to Each Array Element")
    public void testApplyArithmeticOperatorsToEachArrayElement() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(gt("runtime", 1000)),
                addFields().field("genreScores", map("$genres", add(multiply(strLenCP("$$genre"), 2), 1)).as("genre")),
                project().suppressId().include("title").include("genres").include("genreScores"),
                sort().ascending("title")));
    }

    @Test
    @DisplayName("Use $$IDX to Access the Index")
    public void testUseIdxToAccessTheIndex() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(gt("runtime", 1000)),
                project().suppressId().include("title").include("rankedGenres",
                        map("$genres",
                                document().field("genre", "$$genre").field("rank", add("$$IDX", 1)).field("isPrimary", eq("$$IDX", 0)))
                                .as("genre")),
                sort().ascending("title")));
    }
}
