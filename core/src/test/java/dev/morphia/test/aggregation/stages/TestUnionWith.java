package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.test.aggregation.AggregationTest;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.literal;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Set.set;
import static dev.morphia.aggregation.stages.Sort.sort;

public class TestUnionWith extends AggregationTest {
    @Test
    public void testUnionWith() {
        checkMinServerVersion(4.4);
        insert("sales2019q1", parseDocs(
                "{ store: 'A', item: 'Chocolates', quantity: 150 }",
                "{ store: 'B', item: 'Chocolates', quantity: 50 }",
                "{ store: 'A', item: 'Cookies', quantity: 100 }",
                "{ store: 'B', item: 'Cookies', quantity: 120 }",
                "{ store: 'A', item: 'Pie', quantity: 10 }",
                "{ store: 'B', item: 'Pie', quantity: 5 }"));

        insert("sales2019q2", parseDocs(
                "{ store: 'A', item: 'Cheese', quantity: 30 }",
                "{ store: 'B', item: 'Cheese', quantity: 50 }",
                "{ store: 'A', item: 'Chocolates', quantity: 125 }",
                "{ store: 'B', item: 'Chocolates', quantity: 150 }",
                "{ store: 'A', item: 'Cookies', quantity: 200 }",
                "{ store: 'B', item: 'Cookies', quantity: 100 }",
                "{ store: 'B', item: 'Nuts', quantity: 100 }",
                "{ store: 'A', item: 'Pie', quantity: 30 }",
                "{ store: 'B', item: 'Pie', quantity: 25 }"));

        insert("sales2019q3", parseDocs(
                "{ store: 'A', item: 'Cheese', quantity: 50 }",
                "{ store: 'B', item: 'Cheese', quantity: 20 }",
                "{ store: 'A', item: 'Chocolates', quantity: 125 }",
                "{ store: 'B', item: 'Chocolates', quantity: 150 }",
                "{ store: 'A', item: 'Cookies', quantity: 200 }",
                "{ store: 'B', item: 'Cookies', quantity: 100 }",
                "{ store: 'A', item: 'Nuts', quantity: 80 }",
                "{ store: 'B', item: 'Nuts', quantity: 30 }",
                "{ store: 'A', item: 'Pie', quantity: 50 }",
                "{ store: 'B', item: 'Pie', quantity: 75 }"));

        insert("sales2019q4", parseDocs(
                "{ store: 'A', item: 'Cheese', quantity: 100, }",
                "{ store: 'B', item: 'Cheese', quantity: 100}",
                "{ store: 'A', item: 'Chocolates', quantity: 200 }",
                "{ store: 'B', item: 'Chocolates', quantity: 300 }",
                "{ store: 'A', item: 'Cookies', quantity: 500 }",
                "{ store: 'B', item: 'Cookies', quantity: 400 }",
                "{ store: 'A', item: 'Nuts', quantity: 100 }",
                "{ store: 'B', item: 'Nuts', quantity: 200 }",
                "{ store: 'A', item: 'Pie', quantity: 100 }",
                "{ store: 'B', item: 'Pie', quantity: 100 }"));

        List<Document> actual = getDs().aggregate("sales2019q1")
                .set(set().field("_id", literal("2019Q1")))
                .unionWith("sales2019q2", addFields().field("_id", literal("2019Q2")))
                .unionWith("sales2019q3", addFields().field("_id", literal("2019Q3")))
                .unionWith("sales2019q4", addFields().field("_id", literal("2019Q4")))
                .sort(sort().ascending("_id", "store", "item"))
                .execute(Document.class)
                .toList();

        List<Document> expected = parseDocs(
                "{ '_id' : '2019Q1', 'store' : 'A', 'item' : 'Chocolates', 'quantity' : 150 }",
                "{ '_id' : '2019Q1', 'store' : 'A', 'item' : 'Cookies', 'quantity' : 100 }",
                "{ '_id' : '2019Q1', 'store' : 'A', 'item' : 'Pie', 'quantity' : 10 }",
                "{ '_id' : '2019Q1', 'store' : 'B', 'item' : 'Chocolates', 'quantity' : 50 }",
                "{ '_id' : '2019Q1', 'store' : 'B', 'item' : 'Cookies', 'quantity' : 120 }",
                "{ '_id' : '2019Q1', 'store' : 'B', 'item' : 'Pie', 'quantity' : 5 }",
                "{ '_id' : '2019Q2', 'store' : 'A', 'item' : 'Cheese', 'quantity' : 30 }",
                "{ '_id' : '2019Q2', 'store' : 'A', 'item' : 'Chocolates', 'quantity' : 125 }",
                "{ '_id' : '2019Q2', 'store' : 'A', 'item' : 'Cookies', 'quantity' : 200 }",
                "{ '_id' : '2019Q2', 'store' : 'A', 'item' : 'Pie', 'quantity' : 30 }",
                "{ '_id' : '2019Q2', 'store' : 'B', 'item' : 'Cheese', 'quantity' : 50 }",
                "{ '_id' : '2019Q2', 'store' : 'B', 'item' : 'Chocolates', 'quantity' : 150 }",
                "{ '_id' : '2019Q2', 'store' : 'B', 'item' : 'Cookies', 'quantity' : 100 }",
                "{ '_id' : '2019Q2', 'store' : 'B', 'item' : 'Nuts', 'quantity' : 100 }",
                "{ '_id' : '2019Q2', 'store' : 'B', 'item' : 'Pie', 'quantity' : 25 }",
                "{ '_id' : '2019Q3', 'store' : 'A', 'item' : 'Cheese', 'quantity' : 50 }",
                "{ '_id' : '2019Q3', 'store' : 'A', 'item' : 'Chocolates', 'quantity' : 125 }",
                "{ '_id' : '2019Q3', 'store' : 'A', 'item' : 'Cookies', 'quantity' : 200 }",
                "{ '_id' : '2019Q3', 'store' : 'A', 'item' : 'Nuts', 'quantity' : 80 }",
                "{ '_id' : '2019Q3', 'store' : 'A', 'item' : 'Pie', 'quantity' : 50 }",
                "{ '_id' : '2019Q3', 'store' : 'B', 'item' : 'Cheese', 'quantity' : 20 }",
                "{ '_id' : '2019Q3', 'store' : 'B', 'item' : 'Chocolates', 'quantity' : 150 }",
                "{ '_id' : '2019Q3', 'store' : 'B', 'item' : 'Cookies', 'quantity' : 100 }",
                "{ '_id' : '2019Q3', 'store' : 'B', 'item' : 'Nuts', 'quantity' : 30 }",
                "{ '_id' : '2019Q3', 'store' : 'B', 'item' : 'Pie', 'quantity' : 75 }",
                "{ '_id' : '2019Q4', 'store' : 'A', 'item' : 'Cheese', 'quantity' : 100 }",
                "{ '_id' : '2019Q4', 'store' : 'A', 'item' : 'Chocolates', 'quantity' : 200 }",
                "{ '_id' : '2019Q4', 'store' : 'A', 'item' : 'Cookies', 'quantity' : 500 }",
                "{ '_id' : '2019Q4', 'store' : 'A', 'item' : 'Nuts', 'quantity' : 100 }",
                "{ '_id' : '2019Q4', 'store' : 'A', 'item' : 'Pie', 'quantity' : 100 }",
                "{ '_id' : '2019Q4', 'store' : 'B', 'item' : 'Cheese', 'quantity' : 100 }",
                "{ '_id' : '2019Q4', 'store' : 'B', 'item' : 'Chocolates', 'quantity' : 300 }",
                "{ '_id' : '2019Q4', 'store' : 'B', 'item' : 'Cookies', 'quantity' : 400 }",
                "{ '_id' : '2019Q4', 'store' : 'B', 'item' : 'Nuts', 'quantity' : 200 }",
                "{ '_id' : '2019Q4', 'store' : 'B', 'item' : 'Pie', 'quantity' : 100 }");

        assertListEquals(actual, expected);
    }

}
