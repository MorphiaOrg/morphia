package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.first;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.DateExpressions.dayOfYear;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Limit.limit;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.*;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.aggregation.stages.Unwind.unwind;
import static dev.morphia.query.Sort.*;
import static dev.morphia.query.filters.Filters.exists;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.lte;
import static dev.morphia.query.filters.Filters.ne;

public class TestPush extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/push/useInGroupStage
     * 
     */
    @Test
    @DisplayName("Use in ``$group`` Stage")
    public void testUseInGroupStage() {
        testPipeline((aggregation) -> aggregation.pipeline(sort().ascending("date").ascending("item"),
                group(id().field("day", dayOfYear("$date")).field("year", year("$date"))).field("itemsSold",
                        push(document().field("item", "$item").field("quantity", "$quantity")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/push/useInSetWindowFieldsStage
     * 
     */
    @Test
    @DisplayName("Use in ``$setWindowFields`` Stage")
    public void testUseInSetWindowFieldsStage() {
        checkMinServerVersion("5.0.0");
        testPipeline((aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                .sortBy(ascending("orderDate")).output(output("quantitiesForState").operator(push("$quantity")).window()
                        .documents("unbounded", "current"))));
    }

    @Test
    @DisplayName("Collect the First Genre of Each Director's Movies")
    public void testCollectTheFirstGenreOfEachDirectorSMovies() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(gte("year", 2010), lte("year", 2015), exists("genres"), ne("genres", List.of()), exists("directors"),
                        ne("directors", List.of())),
                unwind("$directors"),
                group(id("$directors")).field("genres", push(first("$genres"))),
                sort().ascending("_id"),
                limit(5)));
    }
}
