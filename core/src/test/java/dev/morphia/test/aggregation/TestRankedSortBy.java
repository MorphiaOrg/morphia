package dev.morphia.test.aggregation;

import java.util.List;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.AggregationImpl;
import dev.morphia.test.TestBase;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.bottom;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.topN;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;
import static org.testng.Assert.assertEquals;

/**
 * The ranked accumulators ({@code $top}, {@code $bottom}, {@code $topN}, {@code $bottomN}) take one {@code sortBy}
 * document whose keys are in priority order; the server rejects an array of one-key documents.
 */
public class TestRankedSortBy extends TestBase {
    @Test
    public void severalSortKeysFormOneDocument() {
        checkMinServerVersion("5.2.0");
        getDatabase().getCollection("scores").insertMany(List.of(
                new Document("_id", 1).append("game", "G1").append("score", 10).append("player", "b"),
                new Document("_id", 2).append("game", "G1").append("score", 10).append("player", "a"),
                new Document("_id", 3).append("game", "G1").append("score", 5).append("player", "c")));
        Aggregation<Document> aggregation = getDs().aggregate("scores")
                .group(group(id(field("game")))
                        .field("best", topN(value(2), field("player"), descending("score"), ascending("player")))
                        .field("worst", bottom(field("player"), descending("score"), ascending("player"))));

        Document stage = ((AggregationImpl<Document>) aggregation).pipeline().get(0).get("$group", Document.class);
        Document sortBy = new Document("score", -1L).append("player", 1L);
        assertEquals(stage.get("best", Document.class).get("$topN", Document.class).get("sortBy"), sortBy);
        assertEquals(List.copyOf(stage.get("best", Document.class).get("$topN", Document.class)
                .get("sortBy", Document.class).keySet()), List.of("score", "player"));
        assertEquals(stage.get("worst", Document.class).get("$bottom", Document.class).get("sortBy"), sortBy);

        // The server accepts it, and breaks the tie on score by player.
        Document result = aggregation.execute(Document.class).next();
        assertEquals(result.get("best"), List.of("a", "b"));
        assertEquals(result.get("worst"), "c");
    }
}
