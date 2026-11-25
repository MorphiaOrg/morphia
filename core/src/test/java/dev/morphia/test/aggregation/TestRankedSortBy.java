package dev.morphia.test.aggregation;

import java.util.List;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.AggregationImpl;
import dev.morphia.test.TestBase;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.AggregationOptions.aggregationOptions;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.bottom;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.topN;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The ranked accumulators ({@code $top}, {@code $bottom}, {@code $topN}, {@code $bottomN}) take one {@code sortBy}
 * document whose keys are in priority order; the server rejects an array of one-key documents.
 */
public class TestRankedSortBy extends TestBase {
    @Test
    public void severalSortKeysFormOneDocument() {
        getDatabase().getCollection("scores").insertMany(List.of(
                new Document("_id", 1).append("game", "G1").append("score", 10).append("player", "b"),
                new Document("_id", 2).append("game", "G1").append("score", 10).append("player", "a"),
                new Document("_id", 3).append("game", "G1").append("score", 5).append("player", "c")));
        Aggregation<Document> aggregation = getDs().aggregate(aggregationOptions().collection("scores"))
                .pipeline(group(id("$game"))
                        .field("best", topN(2, "$player", descending("score"), ascending("player")))
                        .field("worst", bottom("$player", descending("score"), ascending("player"))));

        Document stage = ((AggregationImpl<Document>) aggregation).pipeline().get(0).get("$group", Document.class);
        Document sortBy = new Document("score", -1).append("player", 1);
        assertEquals(sortBy, stage.get("best", Document.class).get("$topN", Document.class).get("sortBy"));
        assertEquals(List.of("score", "player"), List.copyOf(
                stage.get("best", Document.class).get("$topN", Document.class).get("sortBy", Document.class).keySet()));
        assertEquals(sortBy, stage.get("worst", Document.class).get("$bottom", Document.class).get("sortBy"));

        // The server accepts it, and breaks the tie on score by player.
        Document result = aggregation.toList().get(0);
        assertEquals(List.of("a", "b"), result.get("best"));
        assertEquals("c", result.get("worst"));
    }
}
