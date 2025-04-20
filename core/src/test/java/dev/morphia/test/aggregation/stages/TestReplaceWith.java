package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.aggregation.stages.Unset;
import dev.morphia.query.Type;
import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Author;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ConditionalExpressions.ifNull;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.of;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.Miscellaneous.setField;
import static dev.morphia.aggregation.expressions.ObjectExpressions.mergeObjects;
import static dev.morphia.aggregation.expressions.SystemVariables.ROOT;
import static dev.morphia.aggregation.stages.ReplaceWith.replaceWith;
import static dev.morphia.query.filters.Filters.exists;
import static dev.morphia.query.filters.Filters.type;
import static java.util.stream.Collectors.toList;

public class TestReplaceWith extends AggregationTest {
    @Test
    public void testReplaceWith() {
        List<Document> documents = parseDocs(
                "{'_id': 1, 'name': {'first': 'John', 'last': 'Backus'}}",
                "{'_id': 2, 'name': {'first': 'John', 'last': 'McCarthy'}}",
                "{'_id': 3, 'name': {'first': 'Grace', 'last': 'Hopper'}}",
                "{'_id': 4, 'firstname': 'Ole-Johan', 'lastname': 'Dahl'}");

        insert("authors", documents);

        List<Document> actual = getDs().aggregate(Author.class)
                .match(exists("name"),
                        type("name", Type.ARRAY).not(),
                        type("name", Type.OBJECT))
                .replaceWith(replaceWith(field("name")))
                .execute(Document.class)
                .toList();
        List<Document> expected = documents.subList(0, 3)
                .stream()
                .map(d -> (Document) d.get("name"))
                .collect(toList());
        assertDocumentEquals(actual, expected);

        actual = getDs().aggregate(Author.class)
                .replaceWith(replaceWith(ifNull()
                        .target(field("name"))
                        .field("_id", field("_id"))
                        .field("missingName", value(true))))
                .execute(Document.class)
                .toList();
        expected = documents.subList(0, 3)
                .stream()
                .map(d -> (Document) d.get("name"))
                .collect(toList());
        expected.add(new Document("_id", 4)
                .append("missingName", true));
        assertDocumentEquals(actual, expected);

        actual = getDs().aggregate(Author.class)
                .replaceWith(replaceWith(mergeObjects()
                        .add(of()
                                .field("_id", field("_id"))
                                .field("first", value(""))
                                .field("last", value("")))
                        .add(field("name"))))
                .execute(Document.class)
                .toList();
        expected = documents.subList(0, 3)
                .stream()
                .peek(d -> d.putAll((Document) d.remove("name")))
                .collect(toList());
        expected.add(new Document("_id", 4)
                .append("first", "")
                .append("last", ""));
        assertDocumentEquals(actual, expected);
    }

    @Test
    public void testSetField() {
        insert("inventory", parseDocs(
                "{ '_id' : 1, 'item' : 'sweatshirt', 'price': 45.99, qty: 300 }",
                "{ '_id' : 2, 'item' : 'winter coat', 'price': 499.99, qty: 200 }",
                "{ '_id' : 3, 'item' : 'sun dress', 'price': 199.99, qty: 250 }",
                "{ '_id' : 4, 'item' : 'leather boots', 'price': 249.99, qty: 300 }",
                "{ '_id' : 5, 'item' : 'bow tie', 'price': 9.99, qty: 180 }"));

        List<Document> actual = getDs().aggregate("inventory")
                .replaceWith(replaceWith(setField("price.usd", ROOT, field("price"))))
                .unset(Unset.unset("price"))
                .execute(Document.class)
                .toList();

        List<Document> expected = parseDocs(
                "{ _id: 1, item: 'sweatshirt', qty: 300, 'price.usd': 45.99 }",
                "{ _id: 2, item: 'winter coat', qty: 200, 'price.usd': 499.99 }",
                "{ _id: 3, item: 'sun dress', qty: 250, 'price.usd': 199.99 }",
                "{ _id: 4, item: 'leather boots', qty: 300, 'price.usd': 249.99 }",
                "{ _id: 5, item: 'bow tie', qty: 180, 'price.usd': 9.99 }");
        assertListEquals(actual, expected);

    }

}
