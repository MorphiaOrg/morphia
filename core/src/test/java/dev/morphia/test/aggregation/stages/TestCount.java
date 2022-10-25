package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Score;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.gt;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;

public class TestCount extends AggregationTest {
    @Test
    public void testCount() {
        insert("scores", List.of(
                parse("{ '_id' : 1, 'subject' : 'History', 'score' : 88 }"),
                parse("{ '_id' : 2, 'subject' : 'History', 'score' : 92 }"),
                parse("{ '_id' : 3, 'subject' : 'History', 'score' : 97 }"),
                parse("{ '_id' : 4, 'subject' : 'History', 'score' : 71 }"),
                parse("{ '_id' : 5, 'subject' : 'History', 'score' : 79 }"),
                parse("{ '_id' : 6, 'subject' : 'History', 'score' : 83 }")));

        Document scores = getDs().aggregate(Score.class)
                .match(gt("score", 80))
                .count("passing_scores")
                .execute(Document.class)
                .next();
        assertEquals(scores, parse("{ \"passing_scores\" : 4 }"));
    }

}
