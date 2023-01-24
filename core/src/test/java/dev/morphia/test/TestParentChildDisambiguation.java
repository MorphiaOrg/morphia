package dev.morphia.test;

import java.util.Map;

import dev.morphia.UpdateOptions;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Property;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestParentChildDisambiguation extends TestBase {

    @Test
    public void testUpdate() {

        getMapper().map(Parent.class, Child.class);

        UpdateOperator[] updateOperators = new UpdateOperator[2];

        updateOperators[0] = UpdateOperators.setOnInsert(Map.of("child", new Child("purple")));
        updateOperators[1] = UpdateOperators.setOnInsert(Map.of("name", "Fred"));

        getDs().find(Parent.class).filter(Filters.eq("name", "Fred")).update(new UpdateOptions().multi(true).upsert(true),
                updateOperators);

        Parent parent = getDs().find(Parent.class).filter(Filters.eq("name", "Fred")).first();

        Assert.assertNotNull(parent);
        Assert.assertNotNull(parent.getChild());
        Assert.assertEquals(parent.getChild().getColor(), "purple");
    }

    private static abstract class MorphiaDurable {

        @Id
        private ObjectId id;

        public ObjectId getId() {

            return id;
        }

        public void setId(ObjectId id) {

            this.id = id;
        }

        @PrePersist
        public void prePersist(Document document) {

            document.remove("aFieldIDoNotWant");
        }
    }

    @Entity(value = "parent", useDiscriminator = false)
    private static class Parent extends MorphiaDurable {

        @Property("child")
        private Child child;
        @Property("name")
        String name;

        public String getName() {

            return name;
        }

        public void setName(String name) {

            this.name = name;
        }

        public Child getChild() {

            return child;
        }

        public void setChild(Child child) {

            this.child = child;
        }
    }

    @Entity
    private static class Child {

        @Property("color")
        private String color;

        public Child(String color) {

            this.color = color;
        }

        public String getColor() {

            return color;
        }

        public void setColor(String color) {

            this.color = color;
        }
    }
}
