package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.maxN;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.in;

public class TestMaxNArrayElement extends TemplatedTestBase {

    @Test
    @DisplayName("Find the Top Two Cast Members")
    public void testFindTheTopTwoCastMembers() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(in("title", List.of("The Godfather", "Spirited Away", "The Dark Knight"))),
                addFields().field("topTwoCast", maxN(2, "$cast")),
                project().suppressId().include("title").include("cast").include("topTwoCast"),
                sort().ascending("title")));
    }
}
