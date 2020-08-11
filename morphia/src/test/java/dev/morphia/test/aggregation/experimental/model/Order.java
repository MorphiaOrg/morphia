package dev.morphia.test.aggregation.experimental.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import java.util.List;

@Entity("orders")
public class Order {
    @Id
    private int id;
    private String item;
    private int price;
    private int quantity;
    private List<Inventory> inventoryDocs;

    private Order() {
    }

    public Order(final int id) {
        this.id = id;
    }

    public Order(final int id, final String item, final int price, final int quantity) {
        this.id = id;
        this.item = item;
        this.price = price;
        this.quantity = quantity;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public List<Inventory> getInventoryDocs() {
        return inventoryDocs;
    }

    public void setInventoryDocs(final List<Inventory> inventoryDocs) {
        this.inventoryDocs = inventoryDocs;
    }

    public String getItem() {
        return item;
    }

    public void setItem(final String item) {
        this.item = item;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(final int price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(final int quantity) {
        this.quantity = quantity;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (item != null ? item.hashCode() : 0);
        result = 31 * result + price;
        result = 31 * result + quantity;
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Order)) {
            return false;
        }

        final Order order = (Order) o;

        if (id != order.id) {
            return false;
        }
        if (price != order.price) {
            return false;
        }
        if (quantity != order.quantity) {
            return false;
        }
        return item != null ? item.equals(order.item) : order.item == null;

    }

}
