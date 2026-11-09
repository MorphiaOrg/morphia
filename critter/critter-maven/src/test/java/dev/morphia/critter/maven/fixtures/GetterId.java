package dev.morphia.critter.maven.fixtures;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * An entity whose {@code @Id} is on a getter, which build-time generation skips.
 */
@Entity
public class GetterId {
    private ObjectId id;

    @Id
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }
}
