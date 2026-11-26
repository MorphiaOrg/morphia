package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.query.Sort;
import dev.morphia.query.Type;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.DateExpressions.dayOfYear;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.in;
import static dev.morphia.query.filters.Filters.type;

public class TestSum extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/sum/useInGroupStage
     * 
     */
    @Test
    @DisplayName("Use in ``$group`` Stage")
    public void testUseInGroupStage() {
        testPipeline((aggregation) -> aggregation
                .pipeline(group(id(document().field("day", dayOfYear("$date")).field("year", year("$date"))))
                        .field("totalAmount", sum(multiply("$price", "$quantity"))).field("count", sum(1))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/sum/useInProjectStage
     * 
     */
    @Test
    @DisplayName("Use in ``$project`` Stage")
    public void testUseInProjectStage() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("quizTotal", sum("$quizzes"))
                .include("labTotal", sum("$labs")).include("examTotal", sum("$final", "$midterm"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/sum/useInSetWindowFieldsStage
     * 
     */
    @Test
    @DisplayName("Use in ``$setWindowFields`` Stage")
    public void testUseInSetWindowFieldsStage() {
        checkMinServerVersion("5.0.0");
        testPipeline((aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                .sortBy(Sort.ascending("orderDate")).output(output("sumQuantityForState").operator(sum("$quantity"))
                        .window().documents("unbounded", "current"))));
    }

    @Test
    @DisplayName("Total the Votes by Rating")
    public void testTotalTheVotesByRating() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("rated", List.of("G", "PG", "PG-13", "R")), type("imdb.votes", Type.INTEGER_32_BIT)),
                group(id("$rated")).field("totalVotes", sum("$imdb.votes")),
                sort().ascending("_id")));
    }
}
