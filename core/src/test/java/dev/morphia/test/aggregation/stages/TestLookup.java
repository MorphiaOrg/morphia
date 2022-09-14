package dev.morphia.test.aggregation.stages;

import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Inventory;
import dev.morphia.test.aggregation.model.Order;
import org.testng.annotations.Test;

import java.util.List;

import static dev.morphia.aggregation.stages.Lookup.lookup;
import static dev.morphia.aggregation.stages.Sort.sort;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

public class TestLookup extends AggregationTest {
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
