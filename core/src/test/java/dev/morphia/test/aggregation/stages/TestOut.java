package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Out.out;
import static dev.morphia.query.filters.Filters.gt;

public class TestOut extends TemplatedTestBase {

    @Test
    @DisplayName("Output to Same Database")
    public void testOutputToSameDatabase() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(gt("runtime", 1000)),
                group(id("$year")).field("movies", push("$title")),
                out("movies_by_year")));
    }
}
