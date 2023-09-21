package dev.morphia.test.lifecycle;

import java.util.Map;

import dev.morphia.UpdateOptions;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PrePersist;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import dev.morphia.test.TestBase;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import static dev.morphia.query.updates.UpdateOperators.setOnInsert;

public class TestParentChildDisambiguation extends TestBase {

    @Test
    public void testUpdate() {

        getMapper().map(Parent.class, Child.class);

        getDs().find(Parent.class)
                .filter(Filters.eq("name", "Fred"))
                .update(new UpdateOptions().multi(true).upsert(true),
                    setOnInsert(Map.of("child", new Child("purple"))),
                    setOnInsert(Map.of("name", "Fred")));

        Parent parent = getDs().find(Parent.class).filter(Filters.eq("name", "Fred")).first();

        Assert.assertNotNull(parent);
        Assert.assertNotNull(parent.child);
        Assert.assertEquals(parent.child.color, "purple");
    }

}

abstract class MorphiaDurable {
    @Id
    ObjectId id;

    @PrePersist
    public void prePersist(Document document) {
        document.remove("aFieldIDoNotWant");
    }
}

@Entity(value = "parent", useDiscriminator = false)
class Parent extends MorphiaDurable {
    Child child;
    String name;
}

@Entity
class Child {
    String color;

    public Child(String color) {
        this.color = color;
    }
}
