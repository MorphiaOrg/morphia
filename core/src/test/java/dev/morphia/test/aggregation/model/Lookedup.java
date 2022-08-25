package dev.morphia.test.aggregation.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import java.util.List;

@Entity("lookups")
public class Lookedup {
    List<Inventory> inventoryDocs;
    @Id
    private int id;
    private String item;
    private int price;
    private int quantity;
}
