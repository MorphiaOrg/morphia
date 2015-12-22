package org.mongodb.morphia;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.List;


public class TestModOperator extends TestBase {
    @Test
    public void mod() {
        getMorphia().map(Inventory.class);
        getDs().save(new Inventory("Flowers", 8));
        getDs().save(new Inventory("Candy", 2));
        getDs().save(new Inventory("Basketballs", 12));

        List<Inventory> list = getDs().createQuery(Inventory.class)
                                      .filter("quantity mod", new Integer[]{4, 0})
                                      .order("name")
                                      .asList();

        Assert.assertEquals(2, list.size());
        Assert.assertEquals("Basketballs", list.get(0).name);
        Assert.assertEquals("Flowers", list.get(1).name);

        list = getDs().createQuery(Inventory.class)
                      .filter("quantity mod", new Integer[]{4, 2})
                      .order("name")
                      .asList();

        Assert.assertEquals(1, list.size());
        Assert.assertEquals("Candy", list.get(0).name);

        list = getDs().createQuery(Inventory.class)
                      .filter("quantity mod", new Integer[]{6, 0})
                      .order("name")
                      .asList();

        Assert.assertEquals(1, list.size());
        Assert.assertEquals("Basketballs", list.get(0).name);

        list = getDs().createQuery(Inventory.class)
                      .field("quantity").mod(4, 0)
                      .order("name")
                      .asList();

        Assert.assertEquals(2, list.size());
        Assert.assertEquals("Basketballs", list.get(0).name);
        Assert.assertEquals("Flowers", list.get(1).name);

        list = getDs().createQuery(Inventory.class)
                      .field("quantity").mod(4, 2)
                      .order("name")
                      .asList();

        Assert.assertEquals(1, list.size());
        Assert.assertEquals("Candy", list.get(0).name);

        list = getDs().createQuery(Inventory.class)
                      .field("quantity").mod(6, 0)
                      .order("name")
                      .asList();

        Assert.assertEquals(1, list.size());
        Assert.assertEquals("Basketballs", list.get(0).name);
    }

    @Entity
    public static class Inventory {
        @Id
        private ObjectId id;
        private Integer quantity;
        private String name;

        public Inventory() {
        }

        public Inventory(final String name, final Integer quantity) {
            this.name = name;
            this.quantity = quantity;
        }

        @Override
        public String toString() {
            return String.format("Inventory{quantity=%d, name='%s'}", quantity, name);
        }
    }
}
