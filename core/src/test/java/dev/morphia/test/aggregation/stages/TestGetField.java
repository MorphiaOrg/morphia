package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.test.aggregation.AggregationTest;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.expressions.Expressions.literal;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.Miscellaneous.getField;
import static dev.morphia.query.filters.Filters.expr;

public class TestGetField extends AggregationTest {
    @Test
    public void testGetField() {
        checkMinServerVersion("5.0.0");

        insert("inventory", parseDocs(
                "{ '_id' : 1, 'item' : 'sweatshirt', 'price_usd': 45.99, qty: 300 }",
                "{ '_id' : 2, 'item' : 'winter coat', 'price_usd': 499.99, qty: 200 }",
                "{ '_id' : 3, 'item' : 'sun dress', 'price_usd': 199.99, qty: 250 }",
                "{ '_id' : 4, 'item' : 'leather boots', 'price_usd': 249.99, qty: 300 }",
                "{ '_id' : 5, 'item' : 'bow tie', 'price_usd': 9.99, qty: 180 }"));

        List<Document> actual = getDs().aggregate("inventory")
                .match(expr(gt(getField("price_usd"), value(200))))
                .execute(Document.class)
                .toList();

        List<Document> expected = parseDocs(
                "{ _id: 2, item: 'winter coat', qty: 200, 'price_usd': 499.99 }",
                "{ _id: 4, item: 'leather boots', qty: 300, 'price_usd': 249.99 }");
        assertListEquals(actual, expected);

        insert("inventory", parseDocs(
                "{ '_id' : 1, 'item' : 'sweatshirt', 'literal_price': 45.99, qty: 300 }",
                "{ '_id' : 2, 'item' : 'winter coat', 'literal_price': 499.99, qty: 200 }",
                "{ '_id' : 3, 'item' : 'sun dress', 'literal_price': 199.99, qty: 250 }",
                "{ '_id' : 4, 'item' : 'leather boots', 'literal_price': 249.99, qty: 300 }",
                "{ '_id' : 5, 'item' : 'bow tie', 'literal_price': 9.99, qty: 180 }"));

        actual = getDs().aggregate("inventory")
                .match(expr(gt(getField(literal("literal_price")), value(200))))
                .execute(Document.class)
                .toList();

        expected = parseDocs(
                "{ _id: 2, item: 'winter coat', qty: 200, 'literal_price': 499.99 }",
                "{ _id: 4, item: 'leather boots', qty: 300, 'literal_price': 249.99 }");
        assertListEquals(actual, expected);

    }

}
