package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.aggregation.stages.Unset;
import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Book;

import org.bson.Document;
import org.testng.annotations.Test;

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
}
