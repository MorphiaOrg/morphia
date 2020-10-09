package dev.morphia.test.aggregation.experimental.expressions;

import com.mongodb.client.MongoCollection;
import dev.morphia.aggregation.experimental.stages.Group;
import org.bson.Document;
import org.testng.annotations.Test;

import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.experimental.expressions.Expressions.meta;
import static dev.morphia.aggregation.experimental.expressions.Expressions.value;
import static dev.morphia.query.experimental.filters.Filters.text;
import static org.bson.Document.parse;

public class ExpressionsTest extends ExpressionsTestBase {

    @Test
    public void testMeta() {
        MongoCollection<Document> articles = getDatabase().getCollection("articles");
        articles.createIndex(new Document("title", "text"));
        articles.insertMany(List.of(
            parse("{ '_id' : 1, 'title' : 'cakes and ale' }"),
            parse("{ '_id' : 2, 'title' : 'more cakes' }"),
            parse("{ '_id' : 3, 'title' : 'bread' }"),
            parse("{ '_id' : 4, 'title' : 'some cakes' }")));

        List<Document> actual = getDs().aggregate("articles")
                                       .match(text("cake"))
                                       .group(Group.of(Group.id(meta()))
                                                   .field("count", sum(value(1))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ '_id' : 0.75, 'count' : 1 }"),
            parse("{ '_id' : 1.0, 'count' : 2 }"));

        assertDocumentEquals(actual, expected);
    }
}