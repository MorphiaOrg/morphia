package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Inventory;
import dev.morphia.test.aggregation.model.Order;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.elementAt;
import static dev.morphia.aggregation.expressions.ArrayExpressions.in;
import static dev.morphia.aggregation.expressions.BooleanExpressions.and;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.ObjectExpressions.mergeObjects;
import static dev.morphia.aggregation.expressions.SystemVariables.ROOT;
import static dev.morphia.aggregation.stages.Lookup.lookup;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.ReplaceRoot.replaceRoot;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.expr;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

public class TestLookup extends AggregationTest {
    @Test
    public void testExample1() {
        loadData("inventory", 2);
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                lookup("inventory")
                        .localField("item")
                        .foreignField("sku")
                        .as("inventory_docs")));
    }

    @Test
    public void testExample2() {
        loadData("members", 2);
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                lookup("members")
                        .localField("enrollmentlist")
                        .foreignField("name")
                        .as("enrollee_info")));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                lookup("items")
                        .localField("item")
                        .foreignField("item")
                        .as("fromItems"),
                replaceRoot(mergeObjects()
                        .add(elementAt("$fromItems", 0))
                        .add(ROOT)),
                project()
                        .exclude("fromItems")));
    }

    @Test
    public void testExample4() {
        loadData("warehouses", 2);
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                lookup("warehouses")
                        .pipeline(
                                match(expr(
                                        and(
                                                ComparisonExpressions.eq("$stock_item", "$$order_item"),
                                                gte("$instock", "$$order_qty")))),
                                project()
                                        .suppressId()
                                        .exclude("stock_item"))
                        .as("stockdata")
                        .let("order_item", "$item")
                        .let("order_qty", "$ordered")));
    }

    @Test
    public void testExample5() {
        loadData("holidays", 2);
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                lookup("holidays")
                        .pipeline(
                                match(eq("year", 2018)),
                                project()
                                        .suppressId()
                                        .include("date", document()
                                                .field("name", "$name")
                                                .field("date", "$date")),
                                replaceRoot("$date"))
                        .as("holidays")));
    }

    @Test
    public void testExample6() {
        loadData("restaurants", 2);
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                lookup("restaurants")
                        .localField("restaurant_name")
                        .foreignField("name")
                        .pipeline(match(expr(in("$$orders_drink", "$beverages"))))
                        .let("orders_drink", "$drink")
                        .as("matches")));
    }

    @Test
    public void testLookup() {
        // Test data pulled from https://docs.mongodb.com/v3.2/reference/operator/aggregation/lookup/
        getDs().save(asList(new Order(1, "abc", 12, 2),
                new Order(2, "jkl", 20, 1),
                new Order(3)));
        List<Inventory> inventories = asList(new Inventory(1, "abc", "product 1", 120),
                new Inventory(2, "def", "product 2", 80),
                new Inventory(3, "ijk", "product 3", 60),
                new Inventory(4, "jkl", "product 4", 70),
                new Inventory(5, null, "Incomplete"),
                new Inventory(6));
        getDs().save(inventories);

        List<Order> lookups = getDs().aggregate(Order.class)
                .lookup(lookup(Inventory.class)
                        .localField("item")
                        .foreignField("sku")
                        .as("inventoryDocs"))
                .sort(sort().ascending("_id"))
                .execute(Order.class)
                .toList();
        assertEquals(lookups.get(0).getInventoryDocs().get(0), inventories.get(0));
        assertEquals(lookups.get(1).getInventoryDocs().get(0), inventories.get(3));
        assertEquals(lookups.get(2).getInventoryDocs().get(0), inventories.get(4));
        assertEquals(lookups.get(2).getInventoryDocs().get(1), inventories.get(5));
    }

}
