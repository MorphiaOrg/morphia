package dev.morphia;


import dev.morphia.mapping.Mapper;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import java.util.List;

import static dev.morphia.query.Sort.ascending;


public class TestModOperator extends TestBase {
    @Test
    public void mod() {
        getMapper().map(Inventory.class);
        getDs().save(new Inventory("Flowers", 8));
        getDs().save(new Inventory("Candy", 2));
        getDs().save(new Inventory("Basketballs", 12));

        List<Inventory> list = getDs().find(Inventory.class)
                                      .filter("quantity mod", new Integer[]{4, 0})
                                      .order(ascending("name"))
                                      .execute().toList();

        Assert.assertEquals(2, list.size());
        Assert.assertEquals("Basketballs", list.get(0).name);
        Assert.assertEquals("Flowers", list.get(1).name);

        list = getDs().find(Inventory.class)
                      .filter("quantity mod", new Integer[]{4, 2})
                      .order(ascending("name"))
                      .execute().toList();

        Assert.assertEquals(1, list.size());
        Assert.assertEquals("Candy", list.get(0).name);

        list = getDs().find(Inventory.class)
                      .filter("quantity mod", new Integer[]{6, 0})
                      .order(ascending("name"))
                      .execute().toList();

        Assert.assertEquals(1, list.size());
        Assert.assertEquals("Basketballs", list.get(0).name);

        list = getDs().find(Inventory.class)
                      .field("quantity").mod(4, 0)
                      .order(ascending("name"))
                      .execute().toList();

        Assert.assertEquals(2, list.size());
        Assert.assertEquals("Basketballs", list.get(0).name);
        Assert.assertEquals("Flowers", list.get(1).name);

        list = getDs().find(Inventory.class)
                      .field("quantity").mod(4, 2)
                      .order(ascending("name"))
                      .execute().toList();

        Assert.assertEquals(1, list.size());
        Assert.assertEquals("Candy", list.get(0).name);

        list = getDs().find(Inventory.class)
                      .field("quantity").mod(6, 0)
                      .order(ascending("name"))
                      .execute().toList();

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
