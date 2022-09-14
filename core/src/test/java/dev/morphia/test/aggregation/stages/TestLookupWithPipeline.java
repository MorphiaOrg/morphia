package dev.morphia.test.aggregation.stages;

import com.mongodb.ReadConcern;
import dev.morphia.aggregation.AggregationOptions;
import dev.morphia.test.aggregation.AggregationTest;
import org.bson.Document;
import org.testng.annotations.Test;

import java.util.List;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.of;
import static dev.morphia.aggregation.stages.Lookup.lookup;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.expr;

public class TestLookupWithPipeline extends AggregationTest {
    @Test
    public void testLookupWithPipeline() {
        // Test data pulled from https://docs.mongodb.com/v3.2/reference/operator/aggregation/lookup/
        insert("orders", parseDocs("{ '_id' : 1, 'item' : 'almonds', 'price' : 12, 'ordered' : 2 }",
                "{ '_id' : 2, 'item' : 'pecans', 'price' : 20, 'ordered' : 1 }",
                "{ '_id' : 3, 'item' : 'cookies', 'price' : 10, 'ordered' : 60 }"));

        insert("warehouses", parseDocs("{ '_id' : 1, 'stock_item' : 'almonds', warehouse: 'A', 'instock' : 120 },",
                "{ '_id' : 2, 'stock_item' : 'pecans', warehouse: 'A', 'instock' : 80 }",
                "{ '_id' : 3, 'stock_item' : 'almonds', warehouse: 'B', 'instock' : 60 }",
                "{ '_id' : 4, 'stock_item' : 'cookies', warehouse: 'B', 'instock' : 40 }",
                "{ '_id' : 5, 'stock_item' : 'cookies', warehouse: 'A', 'instock' : 80 }"));

        List<Document> actual = getDs().aggregate("orders")
                .lookup(lookup("warehouses")
                        .let("order_item", field("item"))
                        .let("order_qty", field("ordered"))
                        .as("stockdata")
                        .pipeline(
                                match(
                                        expr(
                                                of().field(
                                                        "$and",
                                                        array(of()
                                                                .field("$eq",
                                                                        array(field("stock"), field("$order_item"))),
                                                                of()
                                                                        .field("$gte",
                                                                                array(field("instock"), field("$order_qty"))))

                                                ))),
                                project()
                                        .exclude("stock_item")
                                        .exclude("_id")))
                .execute(Document.class, new AggregationOptions().readConcern(ReadConcern.LOCAL))
                .toList();

        List<Document> expected = parseDocs(
                "{ '_id' : 1, 'item' : 'almonds', 'price' : 12, 'ordered' : 2, 'stockdata' : [ { 'warehouse' : 'A', 'instock'" +
                        " : 120 }, { 'warehouse' : 'B', 'instock' : 60 } ] }",
                "{ '_id' : 2, 'item' : 'pecans', 'price' : 20, 'ordered' : 1, 'stockdata' : [ { 'warehouse' : 'A', 'instock' : 80 } ] }",
                "{ '_id' : 3, 'item' : 'cookies', 'price' : 10, 'ordered' : 60, 'stockdata' : [ { 'warehouse' : 'A', 'instock' : 80 } ] }");

        assertDocumentEquals(actual, expected);
    }

}
