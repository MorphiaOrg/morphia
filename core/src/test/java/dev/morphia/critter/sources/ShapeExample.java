package dev.morphia.critter.sources;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * An abstract entity whose private fields are inherited by its subclasses.
 */
@Entity
public abstract class ShapeExample {
    @Id
    private ObjectId id;
    private String color;

    public ObjectId getId() {
        return id;
    }

    public String getColor() {
        return color;
    }
}
