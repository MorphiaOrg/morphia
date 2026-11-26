package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.query.filters.Filters;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.aggregation.model.Inventory;
import dev.morphia.test.aggregation.model.Order;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.elementAt;
import static dev.morphia.aggregation.expressions.ArrayExpressions.in;
import static dev.morphia.aggregation.expressions.BooleanExpressions.and;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
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

public class TestLookup extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/lookup/performASingleEqualityJoinWithLookup
     * 
     */
    @Test
    @DisplayName("Perform a Single Equality Join with ``$lookup``")
    public void testPerformASingleEqualityJoinWithLookup() {
        testPipeline((aggregation) -> aggregation
                .pipeline(lookup("inventory").localField("item").foreignField("sku").as("inventory_docs")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/lookup/useLookupWithAnArray
     * 
     */
    @Test
    @DisplayName("Use ``$lookup`` with an Array")
    public void testUseLookupWithAnArray() {
        testPipeline((aggregation) -> aggregation
                .pipeline(lookup("members").localField("enrollmentlist").foreignField("name").as("enrollee_info")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/lookup/useLookupWithMergeObjects
     * 
     */
    @Test
    @DisplayName("Use ``$lookup`` with ``$mergeObjects``")
    public void testUseLookupWithMergeObjects() {
        testPipeline((aggregation) -> aggregation.pipeline(
                lookup("items").localField("item").foreignField("item").as("fromItems"),
                replaceRoot(mergeObjects().add(elementAt("$fromItems", 0)).add(ROOT)), project().exclude("fromItems")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/lookup/performMultipleJoinsAndACorrelatedSubqueryWithLookup
     * 
     */
    @Test
    @DisplayName("Perform Multiple Joins and a Correlated Subquery with ``$lookup``")
    public void testPerformMultipleJoinsAndACorrelatedSubqueryWithLookup() {
        testPipeline(
                (aggregation) -> aggregation
                        .pipeline(lookup("warehouses")
                                .pipeline(
                                        match(expr(and(ComparisonExpressions.eq("$stock_item", "$$order_item"),
                                                gte("$instock", "$$order_qty")))),
                                        project().suppressId().exclude("stock_item"))
                                .as("stockdata").let("order_item", "$item").let("order_qty", "$ordered")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/lookup/performAnUncorrelatedSubqueryWithLookup
     * 
     */
    @Test
    @DisplayName("Perform an Uncorrelated Subquery with ``$lookup``")
    public void testPerformAnUncorrelatedSubqueryWithLookup() {
        testPipeline((aggregation) -> aggregation.pipeline(lookup("holidays")
                .pipeline(match(eq("year", 2018)),
                        project().suppressId().include("date",
                                document().field("name", "$name").field("date", "$date")),
                        replaceRoot("$date"))
                .as("holidays")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/lookup/performAConciseCorrelatedSubqueryWithLookup
     * 
     */
    @Test
    @DisplayName("Perform a Concise Correlated Subquery with ``$lookup``")
    public void testPerformAConciseCorrelatedSubqueryWithLookup() {
        checkMinServerVersion("6.0.0");
        testPipeline((aggregation) -> aggregation.pipeline(lookup("restaurants").localField("restaurant_name")
                .foreignField("name").pipeline(match(expr(in("$$orders_drink", "$beverages"))))
                .let("orders_drink", "$drink").as("matches")));
    }

    @Test
    public void testLookup() {
        // Test data pulled from
        // https://docs.mongodb.com/v3.2/reference/operator/aggregation/lookup/
        getDs().save(asList(new Order(1, "abc", 12, 2), new Order(2, "jkl", 20, 1), new Order(3)));
        List<Inventory> inventories = asList(new Inventory(1, "abc", "product 1", 120),
                new Inventory(2, "def", "product 2", 80), new Inventory(3, "ijk", "product 3", 60),
                new Inventory(4, "jkl", "product 4", 70), new Inventory(5, null, "Incomplete"), new Inventory(6));
        getDs().save(inventories);

        List<Order> lookups = getDs().aggregate(Order.class)
                .pipeline(lookup(Inventory.class).localField("item").foreignField("sku").as("inventoryDocs"),
                        sort().ascending("_id"))
                .toList();
        Assertions.assertEquals(inventories.get(0), lookups.get(0).getInventoryDocs().get(0));
        Assertions.assertEquals(inventories.get(3), lookups.get(1).getInventoryDocs().get(0));
        Assertions.assertEquals(inventories.get(4), lookups.get(2).getInventoryDocs().get(0));
        Assertions.assertEquals(inventories.get(5), lookups.get(2).getInventoryDocs().get(1));
    }

    @Test
    @DisplayName("Correlate with Local and Foreign Fields")
    public void testCorrelateWithLocalAndForeignFields() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.in("title", List.of("I Don't Kiss", "Lucky Luke", "Mississippi Masala"))),
                lookup("comments").localField("_id").foreignField("movie_id").let("movie_year", "$year")
                        .pipeline(match(expr(gt(year("$date"), "$$movie_year"))), project().suppressId().include("name").include("date"))
                        .as("post_release_comments"),
                project().suppressId().include("title").include("year").include("post_release_comments")));
    }

    @Test
    @DisplayName("Correlate with Variables Only")
    public void testCorrelateWithVariablesOnly() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.in("title", List.of("I Don't Kiss", "Lucky Luke", "Mississippi Masala"))),
                lookup("comments").let("movie_id", "$_id").let("movie_year", "$year")
                        .pipeline(match(expr(and(ComparisonExpressions.eq("$movie_id", "$$movie_id"), gt(year("$date"), "$$movie_year")))),
                                project().suppressId().include("name").include("date"))
                        .as("post_release_comments"),
                project().suppressId().include("title").include("year").include("post_release_comments")));
    }

    @Test
    @DisplayName("Use Multiple Join Conditions and a Correlated Subquery")
    public void testUseMultipleJoinConditionsAndACorrelatedSubquery() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.in("title", List.of("Class Action", "Kafka", "Corpse Bride"))),
                lookup("comments").localField("_id").foreignField("movie_id").let("movie_year", "$year")
                        .pipeline(match(expr(gt(year("$date"), "$$movie_year"))), project().suppressId().include("name").include("date"))
                        .as("post_release_comments"),
                project().suppressId().include("title").include("year").include("post_release_comments")));
    }
}
