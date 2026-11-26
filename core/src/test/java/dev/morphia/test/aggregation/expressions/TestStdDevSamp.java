package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.Type;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.WindowExpressions.stdDevSamp;
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
import static dev.morphia.query.filters.Filters.type;

public class TestStdDevSamp extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/stdDevSamp/useInSetWindowFieldsStage
     * 
     */
    @Test
    @DisplayName("Use in ``$setWindowFields`` Stage")
    public void testUseInSetWindowFieldsStage() {
        checkMinServerVersion("5.0.0");
        testPipeline((aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                .sortBy(ascending("orderDate")).output(output("stdDevSampQuantityForState")
                        .operator(stdDevSamp("$quantity")).window().documents("unbounded", "current"))));
    }

    @Test
    @DisplayName("Find the Directors with the Most Varied Sampled Ratings")
    public void testFindTheDirectorsWithTheMostVariedSampledRatings() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(exists("directors"), type("imdb.rating", Type.DOUBLE)),
                unwind("$directors"),
                group(id("$directors")).field("movieCount", sum(1)).field("stdDevSampRating", stdDevSamp("$imdb.rating")),
                match(gte("movieCount", 10)),
                sort().descending("stdDevSampRating"),
                limit(5)));
    }
}
