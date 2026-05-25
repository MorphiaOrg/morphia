package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TestBase;
import dev.morphia.test.models.Author;

import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.stages.CollectionStats.collStats;

public class TestCollectionStats extends TestBase {
    @Test
    public void testCollectionStats() {
        getDs().save(new Author());
        Document stats = getDs().aggregate(Author.class, Document.class)
                .pipeline(collStats()
                        .histogram(true)
                        .scale(42)
                        .count(true))
                .iterator()
                .tryNext();
        Assertions.assertNotNull(stats);
    }
}
