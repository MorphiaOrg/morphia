package dev.morphia.test.aggregation.stages;

import com.github.zafarkhaja.semver.Version;
import com.mongodb.client.MongoCursor;

import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Author;
import dev.morphia.test.aggregation.model.Book;

import org.bson.Document;
import org.testng.Assert;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Out.to;
import static dev.morphia.query.filters.Filters.eq;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

public class TestOut extends AggregationTest {
    @Test
    public void testOut() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
                new Book("Divine Comedy", "Dante", 1),
                new Book("Eclogues", "Dante", 2),
                new Book("The Odyssey", "Homer", 10),
                new Book("Iliad", "Homer", 10)));

        getDs().aggregate(Book.class)
                .group(group(id("author"))
                        .field("books", push()
                                .single(field("title"))))
                .out(to(Author.class));
        assertEquals(getDs().getCollection(Author.class).countDocuments(), 2);

        getDs().aggregate(Book.class)
                .group(group(id("author"))
                        .field("books", push()
                                .single(field("title"))))
                .out(to("different"));
        assertEquals(getDatabase().getCollection("different").countDocuments(), 2);
    }

    @Test
    public void testOutAlternateDatabase() {
        checkMinServerVersion(Version.valueOf("4.4.0"));
        getDs().save(asList(new Book("The Banquet", "Dante", 2, "Italian", "Sophomore Slump"),
                new Book("Divine Comedy", "Dante", 1, "Not Very Funny", "I mean for a 'comedy'", "Ironic"),
                new Book("Eclogues", "Dante", 2, "Italian", ""),
                new Book("The Odyssey", "Homer", 10, "Classic", "Mythology", "Sequel"),
                new Book("Iliad", "Homer", 10, "Mythology", "Trojan War", "No Sequel")));

        getDs().aggregate(Book.class)
                .match(eq("author", "Homer"))
                .group(group(id("author"))
                        .field("copies", sum(field("copies"))))
                .out(to("testAverage")
                        .database("homer"));

        try (MongoCursor<Document> testAverage = getMongoClient()
                .getDatabase("homer")
                .getCollection("testAverage")
                .find().iterator()) {
            Assert.assertEquals(testAverage.next().get("copies"), 20);
        }
        getMongoClient().getDatabase("homer").drop();
    }

    @Test
    public void testOutNamedCollection() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2, "Italian", "Sophomore Slump"),
                new Book("Divine Comedy", "Dante", 1, "Not Very Funny", "I mean for a 'comedy'", "Ironic"),
                new Book("Eclogues", "Dante", 2, "Italian", ""),
                new Book("The Odyssey", "Homer", 10, "Classic", "Mythology", "Sequel"),
                new Book("Iliad", "Homer", 10, "Mythology", "Trojan War", "No Sequel")));

        getDs().aggregate(Book.class)
                .match(eq("author", "Homer"))
                .group(group(id("author"))
                        .field("copies", sum(field("copies"))))
                .out(to("testAverage"));
        try (MongoCursor<Document> testAverage = getDatabase().getCollection("testAverage").find().iterator()) {
            Assert.assertEquals(testAverage.next().get("copies"), 20);
        }
    }
}
