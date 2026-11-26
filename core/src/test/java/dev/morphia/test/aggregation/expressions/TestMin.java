package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.query.Sort;
import dev.morphia.query.Type;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.min;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.in;
import static dev.morphia.query.filters.Filters.type;

public class TestMin extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/min/useInGroupStage
     * 
     */
    @Test
    @DisplayName("Use in ``$group`` Stage")
    public void testUseInGroupStage() {
        testPipeline((aggregation) -> aggregation.pipeline(group(id("$item")).field("minQuantity", min("$quantity"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/min/useInProjectStage
     * 
     */
    @Test
    @DisplayName("Use in ``$project`` Stage")
    public void testUseInProjectStage() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("quizMin", min("$quizzes"))
                .include("labMin", min("$labs")).include("examMin", min("$final", "$midterm"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/min/useInSetWindowFieldsStage
     * 
     */
    @Test
    @DisplayName("Use in ``$setWindowFields`` Stage")
    public void testUseInSetWindowFieldsStage() {
        checkMinServerVersion("5.0.0");
        testPipeline((aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                .sortBy(Sort.ascending("orderDate")).output(output("minimumQuantityForState")
                        .operator(min("$quantity")).window().documents("unbounded", "current"))));
    }

    @Test
    @DisplayName("Use $min in a $group Stage")
    public void testUseMinInAGroupStage() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("rated", List.of("G", "PG", "PG-13", "R")), type("imdb.rating", Type.DOUBLE)),
                group(id("$rated")).field("lowestRating", min("$imdb.rating")),
                sort().ascending("_id")));
    }
}
