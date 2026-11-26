package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.query.Type;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.in;
import static dev.morphia.query.filters.Filters.type;

public class TestAccumulator extends TemplatedTestBase {

    @Test
    @DisplayName("Comparison with $avg")
    public void testComparisonWithAvg() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("rated", List.of("G", "PG", "PG-13", "R")), type("imdb.rating", Type.DOUBLE)),
                group(id("$rated")).field("avgRating", avg("$imdb.rating")),
                sort().ascending("_id")));
    }
}
