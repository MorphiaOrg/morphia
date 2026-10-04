package dev.morphia.critter.sources;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * An entity whose only {@code @Id} is on a getter, so field-based discovery would miss it.
 */
@Entity
public class GetterIdExample {
    private ObjectId id;
    private String name;

    @Id
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
}
