package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.aggregation.stages.Unset;
import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Book;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Miscellaneous.unsetField;
import static dev.morphia.aggregation.expressions.SystemVariables.ROOT;
import static dev.morphia.aggregation.stages.ReplaceWith.replaceWith;
import static dev.morphia.aggregation.stages.Unset.unset;
import static org.testng.Assert.assertEquals;

public class TestUnset extends AggregationTest {
    @Test
    public void testUnset() {
        List<Document> documents = parseDocs(
                "{'_id': 1, title: 'Antelope Antics', isbn: '0001122223334', author: {last:'An', first: 'Auntie' }, copies: "
                        + "[ {warehouse: 'A', qty: 5 }, {warehouse: 'B', qty: 15 } ] }",
                "{'_id': 2, title: 'Bees Babble', isbn: '999999999333', author: {last:'Bumble', first: 'Bee' }, copies: [ "
                        + "{warehouse: 'A', qty: 2 }, {warehouse: 'B', qty: 5 } ] }");
        insert("books", documents);

        for (Document document : documents) {
            document.remove("copies");
        }

        List<Document> copies = getDs().aggregate(Book.class)
                .unset(Unset.unset("copies"))
                .execute(Document.class)
                .toList();

        assertEquals(documents, copies);

    }

    @Test
    public void testUnsetField() {
        checkMinDriverVersion(4.5);
        insert("inventory", parseDocs(
                "{ '_id' : 1, 'item' : 'sweatshirt', 'price.usd': 45.99, qty: 300 }",
                "{ '_id' : 2, 'item' : 'winter coat', 'price.usd': 499.99, qty: 200 }",
                "{ '_id' : 3, 'item' : 'sun dress', 'price.usd': 199.99, qty: 250 }",
                "{ '_id' : 4, 'item' : 'leather boots', 'price.usd': 249.99, qty: 300 }",
                "{ '_id' : 5, 'item' : 'bow tie', 'price.usd': 9.99, qty: 180 }"));

        List<Document> actual = getDs().aggregate("inventory")
                .replaceWith(replaceWith(unsetField("price.usd", ROOT)))
                .unset(unset("price"))
                .execute(Document.class)
                .toList();

        List<Document> expected = parseDocs(
                "{ _id: 1, item: 'sweatshirt', qty: 300 }",
                "{ _id: 2, item: 'winter coat', qty: 200 }",
                "{ _id: 3, item: 'sun dress', qty: 250, }",
                "{ _id: 4, item: 'leather boots', qty: 300 }",
                "{ _id: 5, item: 'bow tie', qty: 180 }");
        assertListEquals(actual, expected);
    }
}
