package dev.morphia.test.aggregation.stages;

import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.models.Author;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static org.testng.Assert.assertNotNull;

public class TestIndexStats extends AggregationTest {
    @Test
    public void testIndexStats() {
        assertNotNull(getDs().aggregate(Author.class)
                .indexStats()
                .match(eq("name", "books_1"))
                .execute(Document.class)
                .next());
    }

}
