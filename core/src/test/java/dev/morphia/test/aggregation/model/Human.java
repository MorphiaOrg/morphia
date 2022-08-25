package dev.morphia.test.aggregation.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import org.bson.types.ObjectId;

@Entity
public class Human {
    @Property("_id")
    public ObjectId id;
    public String name;
}
