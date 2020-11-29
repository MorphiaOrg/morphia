package dev.morphia.test.aggregation.experimental.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import java.util.List;
import java.util.StringJoiner;

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

    public Order(int id) {
        this.id = id;
    }

    public Order(int id, String item, int price, int quantity) {
        this.id = id;
        this.item = item;
        this.price = price;
        this.quantity = quantity;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Inventory> getInventoryDocs() {
        return inventoryDocs;
    }

    public void setInventoryDocs(List<Inventory> inventoryDocs) {
        this.inventoryDocs = inventoryDocs;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
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
    public boolean equals(Object o) {
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

    @Override
    public String toString() {
        return new StringJoiner(", ", Order.class.getSimpleName() + "[", "]")
                   .add("id=" + id)
                   .add("item='" + item + "'")
                   .add("price=" + price)
                   .add("quantity=" + quantity)
                   .add("inventoryDocs=" + inventoryDocs)
                   .toString();
    }
}
