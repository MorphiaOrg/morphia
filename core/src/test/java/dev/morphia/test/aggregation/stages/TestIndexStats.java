package dev.morphia.test.aggregation.stages;

import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Author;
import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static org.testng.Assert.assertNotNull;

public class TestIndexStats extends AggregationTest {
    @Test
    public void testIndexStats() {
        getDs().getMapper().map(Author.class);
        getDs().ensureIndexes();
        Document stats = getDs().aggregate(Author.class)
                .indexStats()
                .match(eq("name", "books_1"))
                .execute(Document.class)
                .next();

        assertNotNull(stats);
    }

}
